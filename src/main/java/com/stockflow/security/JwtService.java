package com.stockflow.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	public static final String AUTHORITIES_CLAIM = "authorities";

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;
	private final Duration expiration;

	public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
		if (properties.expiration() == null || properties.expiration().isZero()
				|| properties.expiration().isNegative()) {
			throw new IllegalStateException("security.jwt.expiration must be positive");
		}
		this.expiration = properties.expiration();
	}

	public String generateToken(UserDetails userDetails) {
		Instant issuedAt = Instant.now();
		List<String> authorities = userDetails.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.sorted()
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(expiration))
				.claim(AUTHORITIES_CLAIM, authorities)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String extractSubject(String token) {
		return decode(token).getSubject();
	}

	public List<String> extractAuthorities(String token) {
		List<String> authorities = decode(token).getClaimAsStringList(AUTHORITIES_CLAIM);
		return authorities == null ? List.of() : List.copyOf(authorities);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		Jwt jwt = decode(token);
		return userDetails.isEnabled()
				&& userDetails.getUsername().equalsIgnoreCase(jwt.getSubject())
				&& jwt.getExpiresAt() != null
				&& jwt.getExpiresAt().isAfter(Instant.now());
	}

	public long getExpirationSeconds() {
		return expiration.toSeconds();
	}

	private Jwt decode(String token) {
		return jwtDecoder.decode(token);
	}
}
