package com.stockflow.report.dto;

public record LowStockResponse(Long productId, String sku, String productName,
		Integer currentStock, Integer minimumStock) {
}
