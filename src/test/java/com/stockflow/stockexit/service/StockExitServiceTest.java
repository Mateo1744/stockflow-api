package com.stockflow.stockexit.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockExitServiceTest {

	@Mock
	private StockExitRepository stockExitRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private InventoryMovementRepository inventoryMovementRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private StockExitMapper stockExitMapper;

	@InjectMocks
	private StockExitService stockExitService;

	@Test
	void findAllReturnsMappedExits() {
		StockExit stockExit = stockExit(1L, StockExit.Status.DRAFT);
		StockExitResponse response = response(1L, StockExit.Status.DRAFT);
		when(stockExitRepository.findAll()).thenReturn(List.of(stockExit));
		when(stockExitMapper.toResponse(stockExit)).thenReturn(response);

		assertEquals(List.of(response), stockExitService.findAll());
	}

	@Test
	void findByIdReturnsExit() {
		StockExit stockExit = stockExit(1L, StockExit.Status.DRAFT);
		StockExitResponse response = response(1L, StockExit.Status.DRAFT);
		when(stockExitRepository.findById(1L)).thenReturn(Optional.of(stockExit));
		when(stockExitMapper.toResponse(stockExit)).thenReturn(response);

		assertSame(response, stockExitService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenExitDoesNotExist() {
		when(stockExitRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockExitService.findById(99L));
		verifyNoInteractions(stockExitMapper);
	}

	@Test
	void createBuildsDraftWithGeneratedNumberAndLinkedItems() {
		StockExitRequest request = request(List.of(
				new StockExitItemRequest(10L, 3),
				new StockExitItemRequest(20L, 2)
		));
		User user = user(2L);
		Product firstProduct = product(10L, true);
		Product secondProduct = product(20L, true);
		StockExitResponse response = response(5L, StockExit.Status.DRAFT);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(productRepository.findById(10L)).thenReturn(Optional.of(firstProduct));
		when(productRepository.findById(20L)).thenReturn(Optional.of(secondProduct));
		when(stockExitRepository.save(any(StockExit.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(stockExitMapper.toResponse(any(StockExit.class))).thenReturn(response);

		assertSame(response, stockExitService.create(request, 2L));

		ArgumentCaptor<StockExit> captor = ArgumentCaptor.forClass(StockExit.class);
		verify(stockExitRepository).save(captor.capture());
		StockExit saved = captor.getValue();
		assertEquals(StockExit.Status.DRAFT, saved.getStatus());
		assertEquals(StockExit.Reason.DAMAGED, saved.getReason());
		assertSame(user, saved.getPerformedBy());
		assertNotNull(saved.getExitNumber());
		assertEquals(20, saved.getExitNumber().length());
		assertTrue(saved.getExitNumber().matches("[0-9A-F]{20}"));
		assertEquals(2, saved.getItems().size());
		assertTrue(saved.getItems().stream().allMatch(item -> item.getStockExit() == saved));
		verify(stockExitRepository).existsByExitNumber(saved.getExitNumber());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void createThrowsWhenUserDoesNotExist() {
		StockExitRequest request = request(List.of(new StockExitItemRequest(10L, 1)));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockExitService.create(request, 99L));
		verifyNoInteractions(productRepository);
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductDoesNotExist() {
		StockExitRequest request = request(List.of(new StockExitItemRequest(99L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockExitService.create(request, 2L));
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductIsInactive() {
		StockExitRequest request = request(List.of(new StockExitItemRequest(10L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, false)));

		assertThrows(InvalidOperationException.class, () -> stockExitService.create(request, 2L));
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void createRejectsNonPositiveQuantityAtBusinessLayer() {
		StockExitRequest request = request(List.of(new StockExitItemRequest(10L, 0)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

		assertThrows(InvalidOperationException.class, () -> stockExitService.create(request, 2L));
		verifyNoInteractions(productRepository);
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void confirmReducesStockCreatesExitMovementAndConfirmsDocument() {
		User confirmer = user(3L);
		Product product = product(10L, true);
		Inventory inventory = inventory(7L, product, 12);
		StockExit stockExit = exitWithItems(
				5L, StockExit.Status.DRAFT, List.of(item(product, 4))
		);
		StockExitResponse response = response(5L, StockExit.Status.CONFIRMED);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(3L)).thenReturn(Optional.of(confirmer));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		when(stockExitRepository.save(stockExit)).thenReturn(stockExit);
		when(stockExitMapper.toResponse(stockExit)).thenReturn(response);

		assertSame(response, stockExitService.confirm(5L, 3L));

		assertEquals(8, inventory.getCurrentStock());
		assertEquals(StockExit.Status.CONFIRMED, stockExit.getStatus());
		assertNotNull(stockExit.getConfirmedAt());
		assertSame(confirmer, stockExit.getPerformedBy());
		ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
		verify(inventoryMovementRepository).save(captor.capture());
		InventoryMovement movement = captor.getValue();
		assertEquals(InventoryMovement.Type.EXIT, movement.getType());
		assertEquals(4, movement.getQuantity());
		assertEquals(12, movement.getPreviousStock());
		assertEquals(8, movement.getResultingStock());
		assertSame(inventory, movement.getInventory());
		assertSame(confirmer, movement.getPerformedBy());
		assertTrue(movement.getNotes().contains(stockExit.getExitNumber()));
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void confirmRejectsInsufficientStockWithoutApplyingChanges() {
		Product product = product(10L, true);
		Inventory inventory = inventory(7L, product, 2);
		StockExit stockExit = exitWithItems(
				5L, StockExit.Status.DRAFT, List.of(item(product, 4))
		);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

		assertThrows(InsufficientStockException.class, () -> stockExitService.confirm(5L, 2L));
		assertEquals(2, inventory.getCurrentStock());
		assertEquals(StockExit.Status.DRAFT, stockExit.getStatus());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void confirmPrevalidatesEveryItemBeforeApplyingAnyStockChange() {
		Product firstProduct = product(10L, true);
		Product secondProduct = product(20L, true);
		Inventory firstInventory = inventory(7L, firstProduct, 10);
		Inventory secondInventory = inventory(8L, secondProduct, 1);
		StockExit stockExit = exitWithItems(5L, StockExit.Status.DRAFT, List.of(
				item(firstProduct, 3), item(secondProduct, 2)
		));
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(firstInventory));
		when(inventoryRepository.findByProductId(20L)).thenReturn(Optional.of(secondInventory));

		assertThrows(InsufficientStockException.class, () -> stockExitService.confirm(5L, 2L));
		assertEquals(10, firstInventory.getCurrentStock());
		assertEquals(1, secondInventory.getCurrentStock());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void confirmAccumulatesRepeatedProductQuantitiesDuringPrevalidation() {
		Product product = product(10L, true);
		Inventory inventory = inventory(7L, product, 5);
		StockExit stockExit = exitWithItems(5L, StockExit.Status.DRAFT, List.of(
				item(product, 3), item(product, 3)
		));
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

		assertThrows(InsufficientStockException.class, () -> stockExitService.confirm(5L, 2L));
		assertEquals(5, inventory.getCurrentStock());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void confirmIsDeclaredTransactionalForAtomicRollback() throws NoSuchMethodException {
		Method method = StockExitService.class.getMethod("confirm", Long.class, Long.class);

		assertNotNull(method.getAnnotation(Transactional.class));
	}

	@Test
	void confirmThrowsWhenExitIsAlreadyConfirmed() {
		StockExit stockExit = stockExit(5L, StockExit.Status.CONFIRMED);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));

		assertThrows(InvalidOperationException.class, () -> stockExitService.confirm(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void confirmThrowsWhenExitIsCancelled() {
		StockExit stockExit = stockExit(5L, StockExit.Status.CANCELLED);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));

		assertThrows(InvalidOperationException.class, () -> stockExitService.confirm(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void confirmThrowsWhenUserDoesNotExist() {
		Product product = product(10L, true);
		StockExit stockExit = exitWithItems(
				5L, StockExit.Status.DRAFT, List.of(item(product, 1))
		);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockExitService.confirm(5L, 99L));
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
		verify(stockExitRepository, never()).save(any());
	}

	@Test
	void confirmThrowsWhenInventoryDoesNotExist() {
		Product product = product(10L, true);
		StockExit stockExit = exitWithItems(
				5L, StockExit.Status.DRAFT, List.of(item(product, 1))
		);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockExitService.confirm(5L, 2L));
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void cancelChangesDraftToCancelledWithoutTouchingInventory() {
		StockExit stockExit = stockExit(5L, StockExit.Status.DRAFT);
		StockExitResponse response = response(5L, StockExit.Status.CANCELLED);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));
		when(stockExitRepository.save(stockExit)).thenReturn(stockExit);
		when(stockExitMapper.toResponse(stockExit)).thenReturn(response);

		assertSame(response, stockExitService.cancel(5L));
		assertEquals(StockExit.Status.CANCELLED, stockExit.getStatus());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void cancelThrowsWhenExitIsConfirmed() {
		StockExit stockExit = stockExit(5L, StockExit.Status.CONFIRMED);
		when(stockExitRepository.findById(5L)).thenReturn(Optional.of(stockExit));

		assertThrows(InvalidOperationException.class, () -> stockExitService.cancel(5L));
		verify(stockExitRepository, never()).save(any());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	private StockExitRequest request(List<StockExitItemRequest> items) {
		return new StockExitRequest(StockExit.Reason.DAMAGED, "Notes", items);
	}

	private StockExit stockExit(Long id, StockExit.Status status) {
		StockExit stockExit = new StockExit();
		stockExit.setId(id);
		stockExit.setExitNumber("EXT-000000000000000" + id);
		stockExit.setReason(StockExit.Reason.DAMAGED);
		stockExit.setStatus(status);
		stockExit.setItems(new ArrayList<>());
		return stockExit;
	}

	private StockExit exitWithItems(
			Long id,
			StockExit.Status status,
			List<StockExitItem> items
	) {
		StockExit stockExit = stockExit(id, status);
		stockExit.setPerformedBy(user(2L));
		for (StockExitItem item : items) {
			item.setStockExit(stockExit);
			stockExit.getItems().add(item);
		}
		return stockExit;
	}

	private StockExitItem item(Product product, int quantity) {
		StockExitItem item = new StockExitItem();
		item.setProduct(product);
		item.setQuantity(quantity);
		return item;
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setFirstName("User");
		return user;
	}

	private Product product(Long id, boolean active) {
		Product product = new Product();
		product.setId(id);
		product.setSku("SKU-" + id);
		product.setName("Product " + id);
		product.setActive(active);
		return product;
	}

	private Inventory inventory(Long id, Product product, int currentStock) {
		Inventory inventory = new Inventory();
		inventory.setId(id);
		inventory.setProduct(product);
		inventory.setCurrentStock(currentStock);
		return inventory;
	}

	private StockExitResponse response(Long id, StockExit.Status status) {
		return new StockExitResponse(
				id, "EXT-000000000000000" + id, StockExit.Reason.DAMAGED, status,
				2L, "User", "Test", "user@test.com", "Notes", List.of(),
				null, null, null
		);
	}
}
