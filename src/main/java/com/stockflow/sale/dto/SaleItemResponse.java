package com.stockflow.sale.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
		Long id,
		Long productId,
		String productSku,
		String productName,
		Integer quantity,
		BigDecimal unitPrice,
		BigDecimal subtotal
) {
}
