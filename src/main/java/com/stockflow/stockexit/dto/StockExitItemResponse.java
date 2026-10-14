package com.stockflow.stockexit.dto;

public record StockExitItemResponse(
		Long id,
		Long productId,
		String productSku,
		String productName,
		Integer quantity
) {
}
