package com.stockflow.auth.service;

import java.util.Optional;
import java.util.Set;

import com.stockflow.auth.dto.LoginRequest;
import com.stockflow.auth.dto.LoginResponse;
import com.stockflow.security.JwtService;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.entity.User;
import com.stockflow.user.mapper.UserMapper;
import com.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserMapper userMapper;

	@Mock
	private JwtService jwtService;

	@Mock
	private Authentication authentication;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				authenticationManager, userRepository, userMapper, jwtService
		);
	}

	@Test
	void loginAuthenticatesAndReturnsJwtWithSafeUser() {
		UserDetails principal = principal(true);
		User user = user(true);
		UserResponse response = response();
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal);
		when(userRepository.findByEmailIgnoreCase("seller@example.com"))
				.thenReturn(Optional.of(user));
		when(jwtService.generateToken(principal)).thenReturn("signed.jwt.token");
		when(jwtService.getExpirationSeconds()).thenReturn(3600L);
		when(userMapper.toResponse(user)).thenReturn(response);

		LoginResponse result = authService.login(
				new LoginRequest(" Seller@Example.com ", "secret123")
		);

		assertEquals("signed.jwt.token", result.token());
		assertEquals("Bearer", result.tokenType());
		assertEquals(3600L, result.expiresIn());
		assertEquals(response, result.user());
		assertFalse(result.toString().contains("secret123"));
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	void loginConvertsInvalidCredentialsToGenericFailure() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("implementation detail"));

		BadCredentialsException exception = assertThrows(
				BadCredentialsException.class,
				() -> authService.login(new LoginRequest("seller@example.com", "wrong"))
		);

		assertEquals("Invalid credentials", exception.getMessage());
		verify(jwtService, never()).generateToken(any());
	}

	@Test
	void loginRejectsUserMissingAfterAuthentication() {
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal(true));
		when(userRepository.findByEmailIgnoreCase("seller@example.com"))
				.thenReturn(Optional.empty());

		assertThrows(
				BadCredentialsException.class,
				() -> authService.login(new LoginRequest("seller@example.com", "secret123"))
		);
		verify(jwtService, never()).generateToken(any());
	}

	@Test
	void loginRejectsInactiveUserWithoutGeneratingJwt() {
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(principal(true));
		when(userRepository.findByEmailIgnoreCase("seller@example.com"))
				.thenReturn(Optional.of(user(false)));

		assertThrows(
				BadCredentialsException.class,
				() -> authService.login(new LoginRequest("seller@example.com", "secret123"))
		);
		verify(jwtService, never()).generateToken(any());
	}

	private UserDetails principal(boolean enabled) {
		return org.springframework.security.core.userdetails.User
				.withUsername("seller@example.com")
				.password("encoded")
				.authorities(new SimpleGrantedAuthority("ROLE_SELLER"))
				.disabled(!enabled)
				.build();
	}

	private User user(boolean active) {
		User user = new User();
		user.setId(5L);
		user.setEmail("seller@example.com");
		user.setPassword("encoded");
		user.setActive(active);
		return user;
	}

	private UserResponse response() {
		return new UserResponse(
				5L, "Sales", "User", "seller@example.com", true, Set.of(), null, null
		);
	}
}
