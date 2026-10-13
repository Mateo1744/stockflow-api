package com.stockflow.user.dto;

import java.time.Instant;
import java.util.Set;

import com.stockflow.role.dto.RoleResponse;

public record UserResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		Boolean active,
		Set<RoleResponse> roles,
		Instant createdAt,
		Instant updatedAt
) {
}
