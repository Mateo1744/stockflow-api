package com.stockflow.inventory.service;

import java.util.List;
import java.util.Optional;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.mapper.InventoryMovementMapper;
import com.stockflow.inventory.repository.InventoryMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

	@Mock
	private InventoryMovementRepository inventoryMovementRepository;

	@Mock
	private InventoryMovementMapper inventoryMovementMapper;

	private InventoryMovementService inventoryMovementService;

	@BeforeEach
	void setUp() {
		inventoryMovementService = new InventoryMovementService(
				inventoryMovementRepository, inventoryMovementMapper
		);
	}

	@Test
	void findAllMapsMovements() {
		InventoryMovement movement = new InventoryMovement();
		InventoryMovementResponse response = response();
		when(inventoryMovementRepository.findAll()).thenReturn(List.of(movement));
		when(inventoryMovementMapper.toResponse(movement)).thenReturn(response);

		assertEquals(List.of(response), inventoryMovementService.findAll());
	}

	@Test
	void findByIdReturnsMovement() {
		InventoryMovement movement = new InventoryMovement();
		when(inventoryMovementRepository.findById(1L)).thenReturn(Optional.of(movement));
		when(inventoryMovementMapper.toResponse(movement)).thenReturn(response());

		assertEquals(1L, inventoryMovementService.findById(1L).id());
	}

	@Test
	void findByIdRejectsMissingMovement() {
		when(inventoryMovementRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> inventoryMovementService.findById(99L)
		);
	}

	private InventoryMovementResponse response() {
		return new InventoryMovementResponse(
				1L, 2L, 3L, "SKU-1", "Keyboard", InventoryMovement.Type.EXIT,
				2, 10, 8, 7L, "Ana", "Diaz", "ana@example.com", "Sale X", null
		);
	}
}
