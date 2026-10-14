package com.stockflow.sale.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record SaleRequest(
		@Positive Long customerId,
		@NotEmpty List<@Valid SaleItemRequest> items
) {
}
