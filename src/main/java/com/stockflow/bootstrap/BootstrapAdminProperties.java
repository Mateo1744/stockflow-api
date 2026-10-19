package com.stockflow.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockflow.bootstrap.admin")
public record BootstrapAdminProperties(
		boolean enabled,
		String email,
		String password,
		String firstName,
		String lastName
) {
}
