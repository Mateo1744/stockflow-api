package com.stockflow.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

	private static final int MINIMUM_HS256_KEY_BYTES = 32;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
			StockFlowUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public SecretKey jwtSecretKey(JwtProperties properties) {
		if (properties.secret() == null || properties.secret().isBlank()) {
			throw new IllegalStateException("security.jwt.secret must be configured");
		}
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(properties.secret());
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("security.jwt.secret must be valid Base64", exception);
		}
		if (decoded.length < MINIMUM_HS256_KEY_BYTES) {
			throw new IllegalStateException(
					"security.jwt.secret must decode to at least 32 bytes"
			);
		}
		return new SecretKeySpec(decoded, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefault());
		return decoder;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(
				java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
		);
		configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			AuthenticationManager authenticationManager,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http
				.cors(cors -> { })
				.csrf(csrf -> csrf.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authenticationManager(authenticationManager)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/reports/low-stock", "/api/reports/inventory-summary")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers("/api/reports/sales-summary")
								.hasAnyRole("ADMIN", "SELLER")
						.requestMatchers(HttpMethod.GET, "/api/sales/**")
								.hasAnyRole("ADMIN", "SELLER", "INVENTORY_MANAGER")
						.requestMatchers("/api/sales/**").hasAnyRole("ADMIN", "SELLER")
						.requestMatchers("/api/stock-entries/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers("/api/stock-exits/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers("/api/inventory-movements/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers("/api/users/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/categories/**").authenticated()
						.requestMatchers("/api/categories/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers("/api/suppliers/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/customers/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/customers/**")
								.hasAnyRole("ADMIN", "SELLER")
						.requestMatchers(HttpMethod.PUT, "/api/customers/**")
								.hasAnyRole("ADMIN", "SELLER")
						.requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/products/**").authenticated()
						.requestMatchers("/api/products/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/inventory/**")
								.hasAnyRole("ADMIN", "INVENTORY_MANAGER")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
