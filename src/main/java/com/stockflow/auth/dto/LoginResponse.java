package com.stockflow.auth.dto;

import com.stockflow.user.dto.UserResponse;

public record LoginResponse(
		String token,
		String tokenType,
		long expiresIn,
		UserResponse user
) {
}
