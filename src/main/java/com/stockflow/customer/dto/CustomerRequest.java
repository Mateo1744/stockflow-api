package com.stockflow.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
		@NotBlank String documentNumber,
		@NotBlank String firstName,
		@NotBlank String lastName,
		@Email String email,
		String phone,
		String address,
		@NotNull Boolean active
) {
}
