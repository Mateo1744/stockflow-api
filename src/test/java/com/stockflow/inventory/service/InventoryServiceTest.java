package com.stockflow.inventory.service;

import java.util.List;
import java.util.Optional;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.dto.InventoryResponse;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.mapper.InventoryMapper;
import com.stockflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private InventoryMapper inventoryMapper;

	@InjectMocks
	private InventoryService inventoryService;

	@Test
	void findAllReturnsMappedInventories() {
		Inventory inventory = inventory(1L, 10);
		InventoryResponse response = response(1L, 10);
		when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
		when(inventoryMapper.toResponse(inventory)).thenReturn(response);

		assertEquals(List.of(response), inventoryService.findAll());
	}

	@Test
	void findByIdReturnsInventory() {
		Inventory inventory = inventory(1L, 10);
		InventoryResponse response = response(1L, 10);
		when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
		when(inventoryMapper.toResponse(inventory)).thenReturn(response);

		assertSame(response, inventoryService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenInventoryDoesNotExist() {
		when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> inventoryService.findById(99L));
		verifyNoInteractions(inventoryMapper);
	}

	@Test
	void findByProductIdReturnsInventory() {
		Inventory inventory = inventory(1L, 10);
		InventoryResponse response = response(1L, 10);
		when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(inventory));
		when(inventoryMapper.toResponse(inventory)).thenReturn(response);

		assertSame(response, inventoryService.findByProductId(7L));
	}

	@Test
	void findByProductIdThrowsWhenInventoryDoesNotExist() {
		when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> inventoryService.findByProductId(99L)
		);
		verifyNoInteractions(inventoryMapper);
	}

	private Inventory inventory(Long id, int stock) {
		Inventory inventory = new Inventory();
		inventory.setId(id);
		inventory.setCurrentStock(stock);
		inventory.setMinimumStock(2);
		return inventory;
	}

	private InventoryResponse response(Long id, int stock) {
		return new InventoryResponse(id, 7L, "SKU-7", "Product", stock, 2, null);
	}
}
