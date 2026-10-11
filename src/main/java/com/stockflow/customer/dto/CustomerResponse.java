package com.stockflow.customer.dto;

import java.time.Instant;

public record CustomerResponse(
		Long id,
		String documentNumber,
		String firstName,
		String lastName,
		String email,
		String phone,
		String address,
		Boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
