package com.stockflow.sale.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.dto.PageResponse;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.customer.entity.Customer;
import com.stockflow.customer.repository.CustomerRepository;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.repository.InventoryMovementRepository;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.product.entity.Product;
import com.stockflow.product.repository.ProductRepository;
import com.stockflow.sale.dto.SaleItemRequest;
import com.stockflow.sale.dto.SaleRequest;
import com.stockflow.sale.dto.SaleResponse;
import com.stockflow.sale.entity.Sale;
import com.stockflow.sale.entity.SaleItem;
import com.stockflow.sale.mapper.SaleMapper;
import com.stockflow.sale.repository.SaleRepository;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

	private static final int SALE_NUMBER_LENGTH = 20;

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final InventoryRepository inventoryRepository;
	private final InventoryMovementRepository inventoryMovementRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final SaleMapper saleMapper;

	public SaleService(
			SaleRepository saleRepository,
			ProductRepository productRepository,
			InventoryRepository inventoryRepository,
			InventoryMovementRepository inventoryMovementRepository,
			CustomerRepository customerRepository,
			UserRepository userRepository,
			SaleMapper saleMapper
	) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.inventoryRepository = inventoryRepository;
		this.inventoryMovementRepository = inventoryMovementRepository;
		this.customerRepository = customerRepository;
		this.userRepository = userRepository;
		this.saleMapper = saleMapper;
	}

	@Transactional(readOnly = true)
	public List<SaleResponse> findAll() {
		return saleRepository.findAll().stream()
				.map(saleMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<SaleResponse> findAll(String saleNumber, Sale.Status status,
			Long customerId, Long sellerId, Instant from, Instant to, Pageable pageable) {
		Specification<Sale> spec = Specification.allOf(
				contains("saleNumber", saleNumber), equal("status", status),
				equalNested("customer", "id", customerId), equalNested("seller", "id", sellerId),
				from == null ? Specification.unrestricted() :
						(root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from),
				to == null ? Specification.unrestricted() :
						(root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
		return PageResponse.from(saleRepository.findAll(spec, pageable).map(saleMapper::toResponse));
	}

	@Transactional(readOnly = true)
	public SaleResponse findById(Long id) {
		return saleMapper.toResponse(findSaleById(id));
	}

	@Transactional
	public SaleResponse create(SaleRequest request, Long sellerUserId) {
		User seller = findUserById(sellerUserId);
		Customer customer = request.customerId() == null
				? null
				: findCustomerById(request.customerId());
		validateUniqueProducts(request.items());

		Sale sale = new Sale();
		sale.setSaleNumber(generateSaleNumber());
		sale.setStatus(Sale.Status.PENDING);
		sale.setCustomer(customer);
		sale.setSeller(seller);

		BigDecimal totalAmount = BigDecimal.ZERO;
		for (SaleItemRequest itemRequest : request.items()) {
			validatePositiveQuantity(itemRequest.quantity());
			Product product = findActiveProductById(itemRequest.productId());
			BigDecimal unitPrice = product.getSalePrice();
			BigDecimal subtotal = unitPrice.multiply(
					BigDecimal.valueOf(itemRequest.quantity())
			);

			SaleItem item = new SaleItem();
			item.setSale(sale);
			item.setProduct(product);
			item.setQuantity(itemRequest.quantity());
			item.setUnitPrice(unitPrice);
			item.setSubtotal(subtotal);
			sale.getItems().add(item);
			totalAmount = totalAmount.add(subtotal);
		}
		sale.setTotalAmount(totalAmount);

		return saleMapper.toResponse(saleRepository.save(sale));
	}

	@Transactional
	public SaleResponse complete(Long id, Long sellerUserId) {
		Sale sale = findSaleById(id);
		validatePendingStatus(sale);
		User seller = findUserById(sellerUserId);
		Map<Long, Inventory> inventoryByProduct = validateAndLoadInventory(sale);

		for (SaleItem item : sale.getItems()) {
			Inventory inventory = inventoryByProduct.get(item.getProduct().getId());
			int previousStock = inventory.getCurrentStock();
			int resultingStock = previousStock - item.getQuantity();
			if (resultingStock < 0) {
				throw insufficientStock(
						item.getProduct().getId(), previousStock, item.getQuantity()
				);
			}

			inventory.setCurrentStock(resultingStock);
			inventoryRepository.save(inventory);
			inventoryMovementRepository.save(createMovement(
					inventory,
					item.getQuantity(),
					previousStock,
					resultingStock,
					seller,
					"Sale " + sale.getSaleNumber()
			));
		}

		sale.setSeller(seller);
		sale.setStatus(Sale.Status.COMPLETED);
		return saleMapper.toResponse(saleRepository.save(sale));
	}

	@Transactional
	public SaleResponse cancel(Long id) {
		Sale sale = findSaleById(id);
		validatePendingStatus(sale);
		sale.setStatus(Sale.Status.CANCELLED);
		return saleMapper.toResponse(saleRepository.save(sale));
	}

	private Map<Long, Inventory> validateAndLoadInventory(Sale sale) {
		Map<Long, Integer> requiredQuantityByProduct = new HashMap<>();
		for (SaleItem item : sale.getItems()) {
			validatePositiveQuantity(item.getQuantity());
			Long productId = item.getProduct().getId();
			try {
				requiredQuantityByProduct.merge(
						productId, item.getQuantity(), Math::addExact
				);
			} catch (ArithmeticException exception) {
				throw new InvalidOperationException(
						"Sale quantity limit exceeded for product id: " + productId
				);
			}
		}

		Map<Long, Inventory> inventoryByProduct = new HashMap<>();
		for (Long productId : requiredQuantityByProduct.keySet().stream().sorted().toList()) {
			Inventory inventory = findInventoryByProductIdForUpdate(productId);
			int requiredQuantity = requiredQuantityByProduct.get(productId);
			if (inventory.getCurrentStock() < requiredQuantity) {
				throw insufficientStock(
						productId, inventory.getCurrentStock(), requiredQuantity
				);
			}
			inventoryByProduct.put(productId, inventory);
		}
		return inventoryByProduct;
	}

	private Sale findSaleById(Long id) {
		return saleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Sale not found with id: " + id
				));
	}

	private User findUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"User not found with id: " + id
				));
	}

	private Customer findCustomerById(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Customer not found with id: " + id
				));
	}

	private Product findActiveProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Product not found with id: " + id
				));
		if (!Boolean.TRUE.equals(product.getActive())) {
			throw new InvalidOperationException("Product is not active with id: " + id);
		}
		return product;
	}

	private Inventory findInventoryByProductId(Long productId) {
		return inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory not found for product id: " + productId
				));
	}

	private Inventory findInventoryByProductIdForUpdate(Long productId) {
		return inventoryRepository.findByProductIdForUpdate(productId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory not found for product id: " + productId));
	}

	private Specification<Sale> contains(String field, String value) {
		return value == null || value.isBlank() ? Specification.unrestricted() : (root, query, cb) ->
				cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
	}

	private Specification<Sale> equal(String field, Object value) {
		return value == null ? Specification.unrestricted() : (root, query, cb) -> cb.equal(root.get(field), value);
	}

	private Specification<Sale> equalNested(String association, String field, Object value) {
		return value == null ? Specification.unrestricted() :
				(root, query, cb) -> cb.equal(root.get(association).get(field), value);
	}

	private String generateSaleNumber() {
		String saleNumber;
		do {
			saleNumber = UUID.randomUUID().toString().replace("-", "")
					.substring(0, SALE_NUMBER_LENGTH).toUpperCase();
		} while (saleRepository.existsBySaleNumber(saleNumber));
		return saleNumber;
	}

	private void validateUniqueProducts(List<SaleItemRequest> items) {
		Set<Long> productIds = new HashSet<>();
		for (SaleItemRequest item : items) {
			if (!productIds.add(item.productId())) {
				throw new InvalidOperationException(
						"Sale contains duplicate product id: " + item.productId()
				);
			}
		}
	}

	private void validatePendingStatus(Sale sale) {
		if (sale.getStatus() != Sale.Status.PENDING) {
			throw new InvalidOperationException(
					"Sale " + sale.getId() + " must be PENDING but is " + sale.getStatus()
			);
		}
	}

	private void validatePositiveQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidOperationException("Sale quantity must be greater than zero");
		}
	}

	private InsufficientStockException insufficientStock(
			Long productId,
			Integer available,
			Integer requested
	) {
		return new InsufficientStockException(
				"Insufficient stock for product id " + productId
						+ ": available " + available + ", requested " + requested
		);
	}

	private InventoryMovement createMovement(
			Inventory inventory,
			Integer quantity,
			Integer previousStock,
			Integer resultingStock,
			User performedBy,
			String notes
	) {
		InventoryMovement movement = new InventoryMovement();
		movement.setInventory(inventory);
		movement.setType(InventoryMovement.Type.EXIT);
		movement.setQuantity(quantity);
		movement.setPreviousStock(previousStock);
		movement.setResultingStock(resultingStock);
		movement.setPerformedBy(performedBy);
		movement.setNotes(notes);
		return movement;
	}
}
