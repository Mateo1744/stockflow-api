package com.stockflow.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
		Long id,
		Long productId,
		String productSku,
		String productName,
		Integer currentStock,
		Integer minimumStock,
		Instant updatedAt
) {
}
