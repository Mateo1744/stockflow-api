package com.stockflow.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
		Long id,
		String sku,
		String name,
		String description,
		BigDecimal salePrice,
		Boolean active,
		Long categoryId,
		String categoryName,
		Instant createdAt,
		Instant updatedAt
) {
}
