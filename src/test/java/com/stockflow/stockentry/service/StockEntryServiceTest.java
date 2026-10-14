package com.stockflow.stockentry.service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
class StockEntryServiceTest {

	@Mock
	private StockEntryRepository stockEntryRepository;

	@Mock
	private SupplierRepository supplierRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private InventoryMovementRepository inventoryMovementRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private StockEntryMapper stockEntryMapper;

	@InjectMocks
	private StockEntryService stockEntryService;

	@Test
	void findAllReturnsMappedEntries() {
		StockEntry entry = entry(1L, StockEntry.Status.DRAFT);
		StockEntryResponse response = response(1L, StockEntry.Status.DRAFT);
		when(stockEntryRepository.findAll()).thenReturn(List.of(entry));
		when(stockEntryMapper.toResponse(entry)).thenReturn(response);

		assertEquals(List.of(response), stockEntryService.findAll());
	}

	@Test
	void findByIdReturnsEntry() {
		StockEntry entry = entry(1L, StockEntry.Status.DRAFT);
		StockEntryResponse response = response(1L, StockEntry.Status.DRAFT);
		when(stockEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
		when(stockEntryMapper.toResponse(entry)).thenReturn(response);

		assertSame(response, stockEntryService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenEntryDoesNotExist() {
		when(stockEntryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.findById(99L));
		verifyNoInteractions(stockEntryMapper);
	}

	@Test
	void createBuildsDraftItemsSubtotalsAndTotal() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 3, new BigDecimal("2.50")),
				new StockEntryItemRequest(20L, 2, new BigDecimal("4.00"))
		));
		Supplier supplier = supplier(1L);
		User user = user(2L);
		Product firstProduct = product(10L, true);
		Product secondProduct = product(20L, true);
		StockEntryResponse response = response(5L, StockEntry.Status.DRAFT);
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(productRepository.findById(10L)).thenReturn(Optional.of(firstProduct));
		when(productRepository.findById(20L)).thenReturn(Optional.of(secondProduct));
		when(stockEntryRepository.save(any(StockEntry.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(stockEntryMapper.toResponse(any(StockEntry.class))).thenReturn(response);

		assertSame(response, stockEntryService.create(request, 2L));

		ArgumentCaptor<StockEntry> captor = ArgumentCaptor.forClass(StockEntry.class);
		verify(stockEntryRepository).save(captor.capture());
		StockEntry saved = captor.getValue();
		assertEquals(StockEntry.Status.DRAFT, saved.getStatus());
		assertSame(supplier, saved.getSupplier());
		assertSame(user, saved.getReceivedBy());
		assertEquals(new BigDecimal("15.50"), saved.getTotalCost());
		assertEquals(2, saved.getItems().size());
		assertEquals(new BigDecimal("7.50"), saved.getItems().get(0).getSubtotal());
		assertEquals(new BigDecimal("8.00"), saved.getItems().get(1).getSubtotal());
		assertTrue(saved.getItems().stream().allMatch(item -> item.getStockEntry() == saved));
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void createWithNullReferenceSkipsDuplicateCheck() {
		StockEntryRequest request = request(null, List.of(
				new StockEntryItemRequest(10L, 1, BigDecimal.ONE)
		));
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, true)));
		when(stockEntryRepository.save(any(StockEntry.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		stockEntryService.create(request, 2L);

		verify(stockEntryRepository, never())
				.existsBySupplierIdAndReferenceNumber(any(), any());
	}

	@Test
	void createThrowsWhenSupplierDoesNotExist() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 1, BigDecimal.ONE)
		));
		when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.create(request, 2L));
		verifyNoInteractions(userRepository, productRepository);
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenUserDoesNotExist() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 1, BigDecimal.ONE)
		));
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L)));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.create(request, 99L));
		verifyNoInteractions(productRepository);
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductDoesNotExist() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(99L, 1, BigDecimal.ONE)
		));
		stubEntryCreateDependencies();
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.create(request, 2L));
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductIsInactive() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 1, BigDecimal.ONE)
		));
		stubEntryCreateDependencies();
		when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, false)));

		assertThrows(InvalidOperationException.class, () -> stockEntryService.create(request, 2L));
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenReferenceNumberIsDuplicatedForSupplier() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 1, BigDecimal.ONE)
		));
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(stockEntryRepository.existsBySupplierIdAndReferenceNumber(1L, "REF-1"))
				.thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> stockEntryService.create(request, 2L));
		verifyNoInteractions(productRepository);
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void createRejectsNonPositiveQuantityAtBusinessLayer() {
		StockEntryRequest request = request("REF-1", List.of(
				new StockEntryItemRequest(10L, 0, BigDecimal.ONE)
		));
		stubEntryCreateDependencies();

		assertThrows(InvalidOperationException.class, () -> stockEntryService.create(request, 2L));
		verifyNoInteractions(productRepository);
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void confirmIncreasesStockCreatesEntryMovementAndConfirmsDocument() {
		User confirmer = user(3L);
		Product product = product(10L, true);
		Inventory inventory = inventory(7L, product, 12);
		StockEntry entry = entryWithItem(5L, StockEntry.Status.DRAFT, product, 4);
		StockEntryResponse response = response(5L, StockEntry.Status.CONFIRMED);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
		when(userRepository.findById(3L)).thenReturn(Optional.of(confirmer));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		when(stockEntryRepository.save(entry)).thenReturn(entry);
		when(stockEntryMapper.toResponse(entry)).thenReturn(response);

		assertSame(response, stockEntryService.confirm(5L, 3L));

		assertEquals(16, inventory.getCurrentStock());
		assertEquals(StockEntry.Status.CONFIRMED, entry.getStatus());
		assertNotNull(entry.getConfirmedAt());
		assertSame(confirmer, entry.getReceivedBy());
		ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
		verify(inventoryMovementRepository).save(captor.capture());
		InventoryMovement movement = captor.getValue();
		assertEquals(InventoryMovement.Type.ENTRY, movement.getType());
		assertEquals(4, movement.getQuantity());
		assertEquals(12, movement.getPreviousStock());
		assertEquals(16, movement.getResultingStock());
		assertSame(inventory, movement.getInventory());
		assertSame(confirmer, movement.getPerformedBy());
		assertTrue(movement.getNotes().contains("REF-5"));
		verify(inventoryRepository).save(inventory);
	}

	@Test
	void confirmThrowsWhenEntryIsAlreadyConfirmed() {
		StockEntry entry = entry(5L, StockEntry.Status.CONFIRMED);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

		assertThrows(InvalidOperationException.class, () -> stockEntryService.confirm(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void confirmThrowsWhenEntryIsCancelled() {
		StockEntry entry = entry(5L, StockEntry.Status.CANCELLED);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

		assertThrows(InvalidOperationException.class, () -> stockEntryService.confirm(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void confirmThrowsWhenUserDoesNotExist() {
		Product product = product(10L, true);
		StockEntry entry = entryWithItem(5L, StockEntry.Status.DRAFT, product, 4);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.confirm(5L, 99L));
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
		verify(stockEntryRepository, never()).save(any());
	}

	@Test
	void confirmThrowsWhenInventoryDoesNotExist() {
		Product product = product(10L, true);
		StockEntry entry = entryWithItem(5L, StockEntry.Status.DRAFT, product, 4);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> stockEntryService.confirm(5L, 2L));
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void confirmIsDeclaredTransactionalForAtomicRollback() throws NoSuchMethodException {
		Method method = StockEntryService.class.getMethod("confirm", Long.class, Long.class);

		assertNotNull(method.getAnnotation(Transactional.class));
	}

	@Test
	void cancelChangesDraftToCancelledWithoutTouchingInventory() {
		StockEntry entry = entry(5L, StockEntry.Status.DRAFT);
		StockEntryResponse response = response(5L, StockEntry.Status.CANCELLED);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
		when(stockEntryRepository.save(entry)).thenReturn(entry);
		when(stockEntryMapper.toResponse(entry)).thenReturn(response);

		assertSame(response, stockEntryService.cancel(5L));
		assertEquals(StockEntry.Status.CANCELLED, entry.getStatus());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void cancelThrowsWhenEntryIsConfirmed() {
		StockEntry entry = entry(5L, StockEntry.Status.CONFIRMED);
		when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

		assertThrows(InvalidOperationException.class, () -> stockEntryService.cancel(5L));
		verify(stockEntryRepository, never()).save(any());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	private void stubEntryCreateDependencies() {
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
	}

	private StockEntryRequest request(String reference, List<StockEntryItemRequest> items) {
		return new StockEntryRequest(1L, reference, "Notes", items);
	}

	private StockEntry entry(Long id, StockEntry.Status status) {
		StockEntry entry = new StockEntry();
		entry.setId(id);
		entry.setStatus(status);
		entry.setReferenceNumber("REF-" + id);
		entry.setItems(new java.util.ArrayList<>());
		return entry;
	}

	private StockEntry entryWithItem(
			Long id,
			StockEntry.Status status,
			Product product,
			int quantity
	) {
		StockEntry entry = entry(id, status);
		entry.setReceivedBy(user(2L));
		StockEntryItem item = new StockEntryItem();
		item.setStockEntry(entry);
		item.setProduct(product);
		item.setQuantity(quantity);
		entry.getItems().add(item);
		return entry;
	}

	private Supplier supplier(Long id) {
		Supplier supplier = new Supplier();
		supplier.setId(id);
		supplier.setName("Supplier");
		return supplier;
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

	private StockEntryResponse response(Long id, StockEntry.Status status) {
		return new StockEntryResponse(
				id, "REF-" + id, status, 1L, "Supplier", 2L,
				"User", "Test", "user@test.com", BigDecimal.ONE, "Notes",
				List.of(), null, null, null
		);
	}
}
