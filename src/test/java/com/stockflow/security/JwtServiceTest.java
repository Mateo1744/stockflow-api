package com.stockflow.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

	private static final String SECRET =
			"MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

	private JwtService jwtService;
	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		jwtService = service(SECRET, Duration.ofHours(1));
		userDetails = user(true);
	}

	@Test
	void generateTokenCreatesCompactJwt() {
		String token = jwtService.generateToken(userDetails);

		assertNotNull(token);
		assertEquals(3, token.split("\\.").length);
	}

	@Test
	void tokenContainsSubjectAndAuthorities() {
		String token = jwtService.generateToken(userDetails);

		assertEquals("admin@example.com", jwtService.extractSubject(token));
		assertEquals(
				List.of("ROLE_ADMIN", "ROLE_INVENTORY_MANAGER"),
				jwtService.extractAuthorities(token)
		);
	}

	@Test
	void signedUnexpiredTokenIsValidForEnabledMatchingUser() {
		String token = jwtService.generateToken(userDetails);

		assertTrue(jwtService.isTokenValid(token, userDetails));
		assertFalse(jwtService.isTokenValid(token, user(false)));
	}

	@Test
	void expiredTokenIsRejected() {
		JwtEncoder encoder = encoder(SECRET);
		Instant issuedAt = Instant.now().minus(Duration.ofMinutes(10));
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("admin@example.com")
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(Duration.ofMinutes(1)))
				.claim(JwtService.AUTHORITIES_CLAIM, List.of("ROLE_ADMIN"))
				.build();
		String token = encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims
		)).getTokenValue();

		assertThrows(JwtException.class, () -> jwtService.extractSubject(token));
	}

	@Test
	void tokenSignedWithDifferentKeyIsRejected() {
		String token = jwtService.generateToken(userDetails);
		String otherSecret = Base64.getEncoder().encodeToString(
				"different-test-secret-32-bytes!!".getBytes()
		);
		JwtService otherService = service(otherSecret, Duration.ofHours(1));

		assertThrows(JwtException.class, () -> otherService.extractSubject(token));
	}

	private JwtService service(String base64Secret, Duration expiration) {
		SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
		JwtEncoder encoder = encoder(base64Secret);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefault());
		return new JwtService(encoder, decoder, new JwtProperties(base64Secret, expiration));
	}

	private JwtEncoder encoder(String base64Secret) {
		SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
		return NimbusJwtEncoder.withSecretKey(key)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	private UserDetails user(boolean enabled) {
		return User.withUsername("admin@example.com")
				.password("encoded")
				.authorities(
						new SimpleGrantedAuthority("ROLE_INVENTORY_MANAGER"),
						new SimpleGrantedAuthority("ROLE_ADMIN")
				)
				.disabled(!enabled)
				.build();
	}
}
