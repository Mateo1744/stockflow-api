package com.stockflow.stockexit.dto;

import java.util.List;

import com.stockflow.stockexit.entity.StockExit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record StockExitRequest(
		@NotNull StockExit.Reason reason,
		String notes,
		@NotEmpty List<@Valid StockExitItemRequest> items
) {
}
