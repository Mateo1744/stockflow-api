package com.stockflow.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierRequest(
		@NotBlank String name,
		@NotBlank String taxId,
		String contactName,
		@Email String email,
		String phone,
		String address,
		@NotNull Boolean active
) {
}
