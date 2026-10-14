package com.stockflow.stockentry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockflow.stockentry.entity.StockEntry;

public record StockEntryResponse(
		Long id,
		String referenceNumber,
		StockEntry.Status status,
		Long supplierId,
		String supplierName,
		Long receivedById,
		String receivedByFirstName,
		String receivedByLastName,
		String receivedByEmail,
		BigDecimal totalCost,
		String notes,
		List<StockEntryItemResponse> items,
		Instant createdAt,
		Instant updatedAt,
		Instant confirmedAt
) {
}
