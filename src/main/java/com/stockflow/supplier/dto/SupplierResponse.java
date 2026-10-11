package com.stockflow.supplier.dto;

import java.time.Instant;

public record SupplierResponse(
		Long id,
		String name,
		String taxId,
		String contactName,
		String email,
		String phone,
		String address,
		Boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
