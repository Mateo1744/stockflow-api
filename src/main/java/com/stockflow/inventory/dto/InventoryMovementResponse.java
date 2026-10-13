package com.stockflow.inventory.dto;

import java.time.Instant;

import com.stockflow.inventory.entity.InventoryMovement;

public record InventoryMovementResponse(
		Long id,
		Long inventoryId,
		Long productId,
		String productSku,
		String productName,
		InventoryMovement.Type type,
		Integer quantity,
		Integer previousStock,
		Integer resultingStock,
		Long performedById,
		String performedByFirstName,
		String performedByLastName,
		String performedByEmail,
		String notes,
		Instant createdAt
) {
}
