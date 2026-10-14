package com.stockflow.stockentry.dto;

import java.math.BigDecimal;

public record StockEntryItemResponse(
		Long id,
		Long productId,
		String productSku,
		String productName,
		Integer quantity,
		BigDecimal unitCost,
		BigDecimal subtotal
) {
}
