package com.stockflow.stockentry.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.repository.InventoryMovementRepository;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.product.entity.Product;
import com.stockflow.product.repository.ProductRepository;
import com.stockflow.stockentry.dto.StockEntryItemRequest;
import com.stockflow.stockentry.dto.StockEntryRequest;
import com.stockflow.stockentry.dto.StockEntryResponse;
import com.stockflow.stockentry.entity.StockEntry;
import com.stockflow.stockentry.entity.StockEntryItem;
import com.stockflow.stockentry.mapper.StockEntryMapper;
import com.stockflow.stockentry.repository.StockEntryRepository;
import com.stockflow.supplier.entity.Supplier;
import com.stockflow.supplier.repository.SupplierRepository;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockEntryService {

	private final StockEntryRepository stockEntryRepository;
	private final SupplierRepository supplierRepository;
	private final ProductRepository productRepository;
	private final InventoryRepository inventoryRepository;
	private final InventoryMovementRepository inventoryMovementRepository;
	private final UserRepository userRepository;
	private final StockEntryMapper stockEntryMapper;

	public StockEntryService(
			StockEntryRepository stockEntryRepository,
			SupplierRepository supplierRepository,
			ProductRepository productRepository,
			InventoryRepository inventoryRepository,
			InventoryMovementRepository inventoryMovementRepository,
			UserRepository userRepository,
			StockEntryMapper stockEntryMapper
	) {
		this.stockEntryRepository = stockEntryRepository;
		this.supplierRepository = supplierRepository;
		this.productRepository = productRepository;
		this.inventoryRepository = inventoryRepository;
		this.inventoryMovementRepository = inventoryMovementRepository;
		this.userRepository = userRepository;
		this.stockEntryMapper = stockEntryMapper;
	}

	@Transactional(readOnly = true)
	public List<StockEntryResponse> findAll() {
		return stockEntryRepository.findAll().stream()
				.map(stockEntryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public StockEntryResponse findById(Long id) {
		return stockEntryMapper.toResponse(findEntryById(id));
	}

	@Transactional
	public StockEntryResponse create(StockEntryRequest request, Long receivedByUserId) {
		Supplier supplier = findSupplierById(request.supplierId());
		User receivedBy = findUserById(receivedByUserId);
		validateReferenceNumber(request.supplierId(), request.referenceNumber());

		StockEntry entry = new StockEntry();
		entry.setSupplier(supplier);
		entry.setReceivedBy(receivedBy);
		entry.setReferenceNumber(request.referenceNumber());
		entry.setStatus(StockEntry.Status.DRAFT);
		entry.setNotes(request.notes());

		BigDecimal totalCost = BigDecimal.ZERO;
		for (StockEntryItemRequest itemRequest : request.items()) {
			validatePositiveQuantity(itemRequest.quantity());
			validateUnitCost(itemRequest.unitCost());
			Product product = findActiveProductById(itemRequest.productId());
			BigDecimal subtotal = itemRequest.unitCost()
					.multiply(BigDecimal.valueOf(itemRequest.quantity()));

			StockEntryItem item = new StockEntryItem();
			item.setStockEntry(entry);
			item.setProduct(product);
			item.setQuantity(itemRequest.quantity());
			item.setUnitCost(itemRequest.unitCost());
			item.setSubtotal(subtotal);
			entry.getItems().add(item);
			totalCost = totalCost.add(subtotal);
		}
		entry.setTotalCost(totalCost);

		return stockEntryMapper.toResponse(stockEntryRepository.save(entry));
	}

	@Transactional
	public StockEntryResponse confirm(Long id, Long receivedByUserId) {
		StockEntry entry = findEntryById(id);
		validateDraftStatus(entry);
		User receivedBy = findUserById(receivedByUserId);

		for (StockEntryItem item : entry.getItems()) {
			validatePositiveQuantity(item.getQuantity());
			Inventory inventory = findInventoryByProductId(item.getProduct().getId());
			int previousStock = inventory.getCurrentStock();
			int resultingStock;
			try {
				resultingStock = Math.addExact(previousStock, item.getQuantity());
			} catch (ArithmeticException exception) {
				throw new InvalidOperationException(
						"Stock limit exceeded for product id: " + item.getProduct().getId()
				);
			}

			inventory.setCurrentStock(resultingStock);
			inventoryRepository.save(inventory);
			inventoryMovementRepository.save(createMovement(
					inventory,
					item.getQuantity(),
					previousStock,
					resultingStock,
					receivedBy,
					movementNotes(entry)
			));
		}

		entry.setReceivedBy(receivedBy);
		entry.setStatus(StockEntry.Status.CONFIRMED);
		entry.setConfirmedAt(Instant.now());
		return stockEntryMapper.toResponse(stockEntryRepository.save(entry));
	}

	@Transactional
	public StockEntryResponse cancel(Long id) {
		StockEntry entry = findEntryById(id);
		validateDraftStatus(entry);
		entry.setStatus(StockEntry.Status.CANCELLED);
		return stockEntryMapper.toResponse(stockEntryRepository.save(entry));
	}

	private StockEntry findEntryById(Long id) {
		return stockEntryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Stock entry not found with id: " + id
				));
	}

	private Supplier findSupplierById(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Supplier not found with id: " + id
				));
	}

	private User findUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"User not found with id: " + id
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

	private void validateReferenceNumber(Long supplierId, String referenceNumber) {
		if (referenceNumber != null
				&& stockEntryRepository.existsBySupplierIdAndReferenceNumber(
						supplierId, referenceNumber
				)) {
			throw new DuplicateResourceException(
					"Stock entry already exists for supplier id " + supplierId
							+ " with reference number: " + referenceNumber
			);
		}
	}

	private void validateDraftStatus(StockEntry entry) {
		if (entry.getStatus() != StockEntry.Status.DRAFT) {
			throw new InvalidOperationException(
					"Stock entry " + entry.getId() + " must be DRAFT but is " + entry.getStatus()
			);
		}
	}

	private void validatePositiveQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidOperationException("Stock entry quantity must be greater than zero");
		}
	}

	private void validateUnitCost(BigDecimal unitCost) {
		if (unitCost == null || unitCost.signum() < 0) {
			throw new InvalidOperationException("Stock entry unit cost cannot be negative");
		}
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
		movement.setType(InventoryMovement.Type.ENTRY);
		movement.setQuantity(quantity);
		movement.setPreviousStock(previousStock);
		movement.setResultingStock(resultingStock);
		movement.setPerformedBy(performedBy);
		movement.setNotes(notes);
		return movement;
	}

	private String movementNotes(StockEntry entry) {
		if (entry.getReferenceNumber() != null) {
			return "Stock entry reference " + entry.getReferenceNumber();
		}
		return "Stock entry id " + entry.getId();
	}
}
