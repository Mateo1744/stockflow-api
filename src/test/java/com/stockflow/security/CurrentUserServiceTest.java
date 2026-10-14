package com.stockflow.security;

import java.util.Optional;

import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

	@Mock
	private UserRepository userRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getCurrentEmailReturnsAuthenticatedName() {
		authenticate("seller@example.com");

		assertEquals(
				"seller@example.com",
				new CurrentUserService(userRepository).getCurrentEmail()
		);
	}

	@Test
	void getCurrentUserResolvesActiveDomainUser() {
		User user = new User();
		user.setEmail("seller@example.com");
		user.setActive(true);
		authenticate("seller@example.com");
		when(userRepository.findByEmailIgnoreCase("seller@example.com"))
				.thenReturn(Optional.of(user));

		assertSame(user, new CurrentUserService(userRepository).getCurrentUser());
	}

	@Test
	void unauthenticatedAccessIsRejected() {
		assertThrows(
				AuthenticationCredentialsNotFoundException.class,
				() -> new CurrentUserService(userRepository).getCurrentEmail()
		);
	}

	private void authenticate(String email) {
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(email, null, java.util.List.of())
		);
	}
}
