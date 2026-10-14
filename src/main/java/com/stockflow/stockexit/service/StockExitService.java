package com.stockflow.stockexit.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.repository.InventoryMovementRepository;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.product.entity.Product;
import com.stockflow.product.repository.ProductRepository;
import com.stockflow.stockexit.dto.StockExitItemRequest;
import com.stockflow.stockexit.dto.StockExitRequest;
import com.stockflow.stockexit.dto.StockExitResponse;
import com.stockflow.stockexit.entity.StockExit;
import com.stockflow.stockexit.entity.StockExitItem;
import com.stockflow.stockexit.mapper.StockExitMapper;
import com.stockflow.stockexit.repository.StockExitRepository;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockExitService {

	private static final int EXIT_NUMBER_LENGTH = 20;

	private final StockExitRepository stockExitRepository;
	private final ProductRepository productRepository;
	private final InventoryRepository inventoryRepository;
	private final InventoryMovementRepository inventoryMovementRepository;
	private final UserRepository userRepository;
	private final StockExitMapper stockExitMapper;

	public StockExitService(
			StockExitRepository stockExitRepository,
			ProductRepository productRepository,
			InventoryRepository inventoryRepository,
			InventoryMovementRepository inventoryMovementRepository,
			UserRepository userRepository,
			StockExitMapper stockExitMapper
	) {
		this.stockExitRepository = stockExitRepository;
		this.productRepository = productRepository;
		this.inventoryRepository = inventoryRepository;
		this.inventoryMovementRepository = inventoryMovementRepository;
		this.userRepository = userRepository;
		this.stockExitMapper = stockExitMapper;
	}

	@Transactional(readOnly = true)
	public List<StockExitResponse> findAll() {
		return stockExitRepository.findAll().stream()
				.map(stockExitMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public StockExitResponse findById(Long id) {
		return stockExitMapper.toResponse(findExitById(id));
	}

	@Transactional
	public StockExitResponse create(StockExitRequest request, Long performedByUserId) {
		User performedBy = findUserById(performedByUserId);
		StockExit stockExit = new StockExit();
		stockExit.setExitNumber(generateExitNumber());
		stockExit.setReason(request.reason());
		stockExit.setStatus(StockExit.Status.DRAFT);
		stockExit.setPerformedBy(performedBy);
		stockExit.setNotes(request.notes());

		for (StockExitItemRequest itemRequest : request.items()) {
			validatePositiveQuantity(itemRequest.quantity());
			Product product = findActiveProductById(itemRequest.productId());
			StockExitItem item = new StockExitItem();
			item.setStockExit(stockExit);
			item.setProduct(product);
			item.setQuantity(itemRequest.quantity());
			stockExit.getItems().add(item);
		}

		return stockExitMapper.toResponse(stockExitRepository.save(stockExit));
	}

	@Transactional
	public StockExitResponse confirm(Long id, Long performedByUserId) {
		StockExit stockExit = findExitById(id);
		validateDraftStatus(stockExit);
		User performedBy = findUserById(performedByUserId);
		List<InventoryChange> changes = prepareInventoryChanges(stockExit);

		for (InventoryChange change : changes) {
			Inventory inventory = change.inventory();
			inventory.setCurrentStock(change.resultingStock());
			inventoryRepository.save(inventory);
			inventoryMovementRepository.save(createMovement(
					inventory,
					change.quantity(),
					change.previousStock(),
					change.resultingStock(),
					performedBy,
					"Stock exit " + stockExit.getExitNumber()
			));
		}

		stockExit.setPerformedBy(performedBy);
		stockExit.setStatus(StockExit.Status.CONFIRMED);
		stockExit.setConfirmedAt(Instant.now());
		return stockExitMapper.toResponse(stockExitRepository.save(stockExit));
	}

	@Transactional
	public StockExitResponse cancel(Long id) {
		StockExit stockExit = findExitById(id);
		validateDraftStatus(stockExit);
		stockExit.setStatus(StockExit.Status.CANCELLED);
		return stockExitMapper.toResponse(stockExitRepository.save(stockExit));
	}

	private List<InventoryChange> prepareInventoryChanges(StockExit stockExit) {
		List<InventoryChange> changes = new ArrayList<>();
		Map<Long, Integer> nextStockByProduct = new HashMap<>();
		Map<Long, Inventory> inventoryByProduct = new HashMap<>();

		for (StockExitItem item : stockExit.getItems()) {
			validatePositiveQuantity(item.getQuantity());
			Long productId = item.getProduct().getId();
			Inventory inventory = inventoryByProduct.computeIfAbsent(
					productId, this::findInventoryByProductId
			);
			int previousStock = nextStockByProduct.getOrDefault(
					productId, inventory.getCurrentStock()
			);
			if (previousStock < item.getQuantity()) {
				throw new InsufficientStockException(
						"Insufficient stock for product id " + productId
								+ ": available " + previousStock
								+ ", requested " + item.getQuantity()
				);
			}
			int resultingStock = previousStock - item.getQuantity();
			nextStockByProduct.put(productId, resultingStock);
			changes.add(new InventoryChange(
					inventory, item.getQuantity(), previousStock, resultingStock
			));
		}
		return changes;
	}

	private StockExit findExitById(Long id) {
		return stockExitRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Stock exit not found with id: " + id
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

	private String generateExitNumber() {
		String exitNumber;
		do {
			exitNumber = UUID.randomUUID().toString().replace("-", "")
					.substring(0, EXIT_NUMBER_LENGTH).toUpperCase();
		} while (stockExitRepository.existsByExitNumber(exitNumber));
		return exitNumber;
	}

	private void validateDraftStatus(StockExit stockExit) {
		if (stockExit.getStatus() != StockExit.Status.DRAFT) {
			throw new InvalidOperationException(
					"Stock exit " + stockExit.getId() + " must be DRAFT but is "
							+ stockExit.getStatus()
			);
		}
	}

	private void validatePositiveQuantity(Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new InvalidOperationException("Stock exit quantity must be greater than zero");
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
		movement.setType(InventoryMovement.Type.EXIT);
		movement.setQuantity(quantity);
		movement.setPreviousStock(previousStock);
		movement.setResultingStock(resultingStock);
		movement.setPerformedBy(performedBy);
		movement.setNotes(notes);
		return movement;
	}

	private record InventoryChange(
			Inventory inventory,
			Integer quantity,
			Integer previousStock,
			Integer resultingStock
	) {
	}
}
