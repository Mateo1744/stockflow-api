package com.stockflow.security;

import java.util.Optional;
import java.util.Set;

import com.stockflow.role.entity.Role;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockFlowUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@Test
	void loadUserMapsEmailPasswordAndRoleAuthorities() {
		User user = user(true);
		when(userRepository.findByEmailIgnoreCase("admin@example.com"))
				.thenReturn(Optional.of(user));

		UserDetails result = new StockFlowUserDetailsService(userRepository)
				.loadUserByUsername("admin@example.com");

		assertEquals("admin@example.com", result.getUsername());
		assertEquals("bcrypt-hash", result.getPassword());
		assertTrue(result.isEnabled());
		assertEquals(
				Set.of("ROLE_ADMIN", "ROLE_SELLER"),
				result.getAuthorities().stream()
						.map(authority -> authority.getAuthority())
						.collect(java.util.stream.Collectors.toSet())
		);
	}

	@Test
	void inactiveDomainUserProducesDisabledPrincipal() {
		User user = user(false);
		when(userRepository.findByEmailIgnoreCase("admin@example.com"))
				.thenReturn(Optional.of(user));

		UserDetails result = new StockFlowUserDetailsService(userRepository)
				.loadUserByUsername("admin@example.com");

		assertFalse(result.isEnabled());
	}

	@Test
	void missingUserUsesGenericAuthenticationError() {
		when(userRepository.findByEmailIgnoreCase("missing@example.com"))
				.thenReturn(Optional.empty());

		UsernameNotFoundException exception = assertThrows(
				UsernameNotFoundException.class,
				() -> new StockFlowUserDetailsService(userRepository)
						.loadUserByUsername("missing@example.com")
		);
		assertEquals("Invalid credentials", exception.getMessage());
	}

	private User user(boolean active) {
		Role admin = new Role();
		admin.setName("ADMIN");
		Role seller = new Role();
		seller.setName("SELLER");

		User user = new User();
		user.setId(1L);
		user.setEmail("admin@example.com");
		user.setPassword("bcrypt-hash");
		user.setActive(active);
		user.setRoles(Set.of(admin, seller));
		return user;
	}
}
