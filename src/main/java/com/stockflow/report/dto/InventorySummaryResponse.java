package com.stockflow.report.dto;

public record InventorySummaryResponse(long totalProducts, long totalUnitsInStock,
		long lowStockProducts) {
}
