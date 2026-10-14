package com.stockflow.stockexit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockExitItemRequest(
		@NotNull @Positive Long productId,
		@NotNull @Positive Integer quantity
) {
}
