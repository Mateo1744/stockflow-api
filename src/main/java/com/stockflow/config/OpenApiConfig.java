package com.stockflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI stockFlowOpenApi() {
		return new OpenAPI()
				.info(new Info().title("StockFlow API")
						.description("REST API for inventory, stock operations and sales management.")
						.version("1.0.0"))
				.components(new Components().addSecuritySchemes(BEARER_AUTH,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}
}
