package com.stockflow.stockexit.dto;

import java.time.Instant;
import java.util.List;

import com.stockflow.stockexit.entity.StockExit;

public record StockExitResponse(
		Long id,
		String exitNumber,
		StockExit.Reason reason,
		StockExit.Status status,
		Long performedById,
		String performedByFirstName,
		String performedByLastName,
		String performedByEmail,
		String notes,
		List<StockExitItemResponse> items,
		Instant createdAt,
		Instant updatedAt,
		Instant confirmedAt
) {
}
