package com.stockflow.sale.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaleRequest(
		Long customerId,
		@NotEmpty List<@Valid SaleItemRequest> items
) {
}
