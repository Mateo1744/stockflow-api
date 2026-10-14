package com.stockflow.stockentry.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockEntryRequest(
		@NotNull @Positive Long supplierId,
		String referenceNumber,
		String notes,
		@NotEmpty List<@Valid StockEntryItemRequest> items
) {
}
