package com.stockflow.sale.service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.stockflow.common.exception.InsufficientStockException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

	@Mock
	private SaleRepository saleRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private InventoryMovementRepository inventoryMovementRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SaleMapper saleMapper;

	@InjectMocks
	private SaleService saleService;

	@Test
	void findAllReturnsMappedSales() {
		Sale sale = sale(1L, Sale.Status.PENDING);
		SaleResponse response = response(1L, Sale.Status.PENDING);
		when(saleRepository.findAll()).thenReturn(List.of(sale));
		when(saleMapper.toResponse(sale)).thenReturn(response);

		assertEquals(List.of(response), saleService.findAll());
	}

	@Test
	void findByIdReturnsSale() {
		Sale sale = sale(1L, Sale.Status.PENDING);
		SaleResponse response = response(1L, Sale.Status.PENDING);
		when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
		when(saleMapper.toResponse(sale)).thenReturn(response);

		assertSame(response, saleService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenSaleDoesNotExist() {
		when(saleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.findById(99L));
		verifyNoInteractions(saleMapper);
	}

	@Test
	void createBuildsPendingSaleWithPriceSnapshotsSubtotalsAndTotal() {
		SaleRequest request = request(3L, List.of(
				new SaleItemRequest(10L, 3),
				new SaleItemRequest(20L, 2)
		));
		User seller = user(2L);
		Customer customer = customer(3L);
		Product firstProduct = product(10L, true, "2.50");
		Product secondProduct = product(20L, true, "4.00");
		SaleResponse response = response(5L, Sale.Status.PENDING);
		when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
		when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
		when(productRepository.findById(10L)).thenReturn(Optional.of(firstProduct));
		when(productRepository.findById(20L)).thenReturn(Optional.of(secondProduct));
		when(saleRepository.save(any(Sale.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(saleMapper.toResponse(any(Sale.class))).thenReturn(response);

		assertSame(response, saleService.create(request, 2L));

		ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
		verify(saleRepository).save(captor.capture());
		Sale saved = captor.getValue();
		assertEquals(Sale.Status.PENDING, saved.getStatus());
		assertSame(seller, saved.getSeller());
		assertSame(customer, saved.getCustomer());
		assertNotNull(saved.getSaleNumber());
		assertEquals(20, saved.getSaleNumber().length());
		assertTrue(saved.getSaleNumber().matches("[0-9A-F]{20}"));
		assertEquals(new BigDecimal("15.50"), saved.getTotalAmount());
		assertEquals(2, saved.getItems().size());
		assertEquals(new BigDecimal("2.50"), saved.getItems().get(0).getUnitPrice());
		assertEquals(new BigDecimal("7.50"), saved.getItems().get(0).getSubtotal());
		assertEquals(new BigDecimal("4.00"), saved.getItems().get(1).getUnitPrice());
		assertEquals(new BigDecimal("8.00"), saved.getItems().get(1).getSubtotal());
		assertTrue(saved.getItems().stream().allMatch(item -> item.getSale() == saved));
		verify(saleRepository).existsBySaleNumber(saved.getSaleNumber());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void createAllowsNullCustomer() {
		SaleRequest request = request(null, List.of(new SaleItemRequest(10L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(10L))
				.thenReturn(Optional.of(product(10L, true, "5.00")));
		when(saleRepository.save(any(Sale.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		saleService.create(request, 2L);

		ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
		verify(saleRepository).save(captor.capture());
		assertNull(captor.getValue().getCustomer());
		verifyNoInteractions(customerRepository);
	}

	@Test
	void createThrowsWhenSellerDoesNotExist() {
		SaleRequest request = request(null, List.of(new SaleItemRequest(10L, 1)));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.create(request, 99L));
		verifyNoInteractions(customerRepository, productRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenCustomerDoesNotExist() {
		SaleRequest request = request(99L, List.of(new SaleItemRequest(10L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.create(request, 2L));
		verifyNoInteractions(productRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductDoesNotExist() {
		SaleRequest request = request(null, List.of(new SaleItemRequest(99L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.create(request, 2L));
		verify(saleRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenProductIsInactive() {
		SaleRequest request = request(null, List.of(new SaleItemRequest(10L, 1)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(productRepository.findById(10L))
				.thenReturn(Optional.of(product(10L, false, "5.00")));

		assertThrows(InvalidOperationException.class, () -> saleService.create(request, 2L));
		verify(saleRepository, never()).save(any());
	}

	@Test
	void createRejectsNonPositiveQuantityAtBusinessLayer() {
		SaleRequest request = request(null, List.of(new SaleItemRequest(10L, 0)));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

		assertThrows(InvalidOperationException.class, () -> saleService.create(request, 2L));
		verifyNoInteractions(productRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void createRejectsDuplicateProductIds() {
		SaleRequest request = request(null, List.of(
				new SaleItemRequest(10L, 1), new SaleItemRequest(10L, 2)
		));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

		InvalidOperationException exception = assertThrows(
				InvalidOperationException.class,
				() -> saleService.create(request, 2L)
		);
		assertTrue(exception.getMessage().contains("duplicate product id: 10"));
		verifyNoInteractions(productRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void completeReducesStockCreatesExitMovementAndCompletesSale() {
		User seller = user(3L);
		Product product = product(10L, true, "99.99");
		Inventory inventory = inventory(7L, product, 12);
		SaleItem item = item(product, 4, "5.00");
		Sale sale = saleWithItems(5L, Sale.Status.PENDING, List.of(item));
		SaleResponse response = response(5L, Sale.Status.COMPLETED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		when(saleRepository.save(sale)).thenReturn(sale);
		when(saleMapper.toResponse(sale)).thenReturn(response);

		assertSame(response, saleService.complete(5L, 3L));

		assertEquals(8, inventory.getCurrentStock());
		assertEquals(Sale.Status.COMPLETED, sale.getStatus());
		assertSame(seller, sale.getSeller());
		assertEquals(new BigDecimal("5.00"), item.getUnitPrice());
		ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(
				InventoryMovement.class
		);
		verify(inventoryMovementRepository).save(captor.capture());
		InventoryMovement movement = captor.getValue();
		assertEquals(InventoryMovement.Type.EXIT, movement.getType());
		assertEquals(4, movement.getQuantity());
		assertEquals(12, movement.getPreviousStock());
		assertEquals(8, movement.getResultingStock());
		assertSame(inventory, movement.getInventory());
		assertSame(seller, movement.getPerformedBy());
		assertTrue(movement.getNotes().contains(sale.getSaleNumber()));
		verify(inventoryRepository).save(inventory);
		verifyNoInteractions(productRepository);
	}

	@Test
	void completeThrowsWhenSaleDoesNotExist() {
		when(saleRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.complete(99L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void completeThrowsWhenSellerDoesNotExist() {
		Sale sale = sale(5L, Sale.Status.PENDING);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.complete(5L, 99L));
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void completeRejectsAlreadyCompletedSale() {
		Sale sale = sale(5L, Sale.Status.COMPLETED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));

		assertThrows(InvalidOperationException.class, () -> saleService.complete(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void completeRejectsCancelledSale() {
		Sale sale = sale(5L, Sale.Status.CANCELLED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));

		assertThrows(InvalidOperationException.class, () -> saleService.complete(5L, 2L));
		verifyNoInteractions(userRepository, inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void completeThrowsWhenInventoryDoesNotExist() {
		Product product = product(10L, true, "5.00");
		Sale sale = saleWithItems(
				5L, Sale.Status.PENDING, List.of(item(product, 1, "5.00"))
		);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.complete(5L, 2L));
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void completeRejectsInsufficientStockWithoutApplyingChanges() {
		Product product = product(10L, true, "5.00");
		Inventory inventory = inventory(7L, product, 2);
		Sale sale = saleWithItems(
				5L, Sale.Status.PENDING, List.of(item(product, 4, "5.00"))
		);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

		assertThrows(InsufficientStockException.class, () -> saleService.complete(5L, 2L));
		assertEquals(2, inventory.getCurrentStock());
		assertEquals(Sale.Status.PENDING, sale.getStatus());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
		verify(saleRepository, never()).save(any());
	}

	@Test
	void completeAggregatesRepeatedProductsBeforeChangingStock() {
		Product product = product(10L, true, "5.00");
		Inventory inventory = inventory(7L, product, 5);
		Sale sale = saleWithItems(5L, Sale.Status.PENDING, List.of(
				item(product, 3, "5.00"), item(product, 3, "5.00")
		));
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));

		assertThrows(InsufficientStockException.class, () -> saleService.complete(5L, 2L));
		assertEquals(5, inventory.getCurrentStock());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void completePrevalidatesEveryProductBeforeApplyingAnyChange() {
		Product firstProduct = product(10L, true, "5.00");
		Product secondProduct = product(20L, true, "8.00");
		Inventory firstInventory = inventory(7L, firstProduct, 10);
		Inventory secondInventory = inventory(8L, secondProduct, 1);
		Sale sale = saleWithItems(5L, Sale.Status.PENDING, List.of(
				item(firstProduct, 3, "5.00"), item(secondProduct, 2, "8.00")
		));
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
		when(inventoryRepository.findByProductId(any()))
				.thenAnswer(invocation -> invocation.<Long>getArgument(0).equals(10L)
						? Optional.of(firstInventory)
						: Optional.of(secondInventory));

		assertThrows(InsufficientStockException.class, () -> saleService.complete(5L, 2L));
		assertEquals(10, firstInventory.getCurrentStock());
		assertEquals(1, secondInventory.getCurrentStock());
		verify(inventoryRepository, never()).save(any());
		verifyNoInteractions(inventoryMovementRepository);
	}

	@Test
	void completeIsDeclaredTransactionalForAtomicRollback() throws NoSuchMethodException {
		Method method = SaleService.class.getMethod("complete", Long.class, Long.class);

		assertNotNull(method.getAnnotation(Transactional.class));
	}

	@Test
	void cancelChangesPendingSaleToCancelledWithoutTouchingInventory() {
		Sale sale = sale(5L, Sale.Status.PENDING);
		SaleResponse response = response(5L, Sale.Status.CANCELLED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));
		when(saleRepository.save(sale)).thenReturn(sale);
		when(saleMapper.toResponse(sale)).thenReturn(response);

		assertSame(response, saleService.cancel(5L));
		assertEquals(Sale.Status.CANCELLED, sale.getStatus());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void cancelRejectsCompletedSale() {
		Sale sale = sale(5L, Sale.Status.COMPLETED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));

		assertThrows(InvalidOperationException.class, () -> saleService.cancel(5L));
		verify(saleRepository, never()).save(any());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	@Test
	void cancelRejectsAlreadyCancelledSale() {
		Sale sale = sale(5L, Sale.Status.CANCELLED);
		when(saleRepository.findById(5L)).thenReturn(Optional.of(sale));

		assertThrows(InvalidOperationException.class, () -> saleService.cancel(5L));
		verify(saleRepository, never()).save(any());
		verifyNoInteractions(inventoryRepository, inventoryMovementRepository);
	}

	private SaleRequest request(Long customerId, List<SaleItemRequest> items) {
		return new SaleRequest(customerId, items);
	}

	private Sale sale(Long id, Sale.Status status) {
		Sale sale = new Sale();
		sale.setId(id);
		sale.setSaleNumber("A7FC92D9173848F482AB");
		sale.setStatus(status);
		sale.setItems(new ArrayList<>());
		return sale;
	}

	private Sale saleWithItems(Long id, Sale.Status status, List<SaleItem> items) {
		Sale sale = sale(id, status);
		sale.setSeller(user(2L));
		for (SaleItem item : items) {
			item.setSale(sale);
			sale.getItems().add(item);
		}
		return sale;
	}

	private SaleItem item(Product product, int quantity, String unitPrice) {
		SaleItem item = new SaleItem();
		item.setProduct(product);
		item.setQuantity(quantity);
		item.setUnitPrice(new BigDecimal(unitPrice));
		item.setSubtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
		return item;
	}

	private Product product(Long id, boolean active, String salePrice) {
		Product product = new Product();
		product.setId(id);
		product.setSku("SKU-" + id);
		product.setName("Product " + id);
		product.setActive(active);
		product.setSalePrice(new BigDecimal(salePrice));
		return product;
	}

	private Inventory inventory(Long id, Product product, int currentStock) {
		Inventory inventory = new Inventory();
		inventory.setId(id);
		inventory.setProduct(product);
		inventory.setCurrentStock(currentStock);
		return inventory;
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setFirstName("Seller");
		return user;
	}

	private Customer customer(Long id) {
		Customer customer = new Customer();
		customer.setId(id);
		customer.setDocumentNumber("DOC-" + id);
		return customer;
	}

	private SaleResponse response(Long id, Sale.Status status) {
		return new SaleResponse(
				id, "A7FC92D9173848F482AB", status, null, null, null, null, null,
				2L, "Seller", "Test", "seller@test.com", BigDecimal.TEN,
				List.of(), null, null
		);
	}
}
