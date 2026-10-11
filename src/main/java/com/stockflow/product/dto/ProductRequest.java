package com.stockflow.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
		@NotBlank String sku,
		@NotBlank String name,
		String description,
		@NotNull @PositiveOrZero BigDecimal salePrice,
		@NotNull Boolean active,
		@NotNull Long categoryId
) {
}
