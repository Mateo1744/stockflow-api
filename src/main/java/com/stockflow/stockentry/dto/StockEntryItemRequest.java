package com.stockflow.stockentry.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record StockEntryItemRequest(
		@NotNull @Positive Long productId,
		@NotNull @Positive Integer quantity,
		@NotNull @PositiveOrZero BigDecimal unitCost
) {
}
