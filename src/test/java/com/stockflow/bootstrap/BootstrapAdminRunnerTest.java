package com.stockflow.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.stockflow.role.entity.Role;
import com.stockflow.role.repository.RoleRepository;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {
	@Mock UserRepository userRepository;
	@Mock RoleRepository roleRepository;
	@Mock ApplicationArguments arguments;

	@Test
	void disabledDoesNotAccessRepositoriesOrCreateUser() {
		runner(new BootstrapAdminProperties(false, null, null, null, null)).run(arguments);
		verify(userRepository, never()).save(any());
		verify(userRepository, never()).existsByRolesName(any());
	}

	@Test
	void enabledCreatesNormalizedAdminWithBcryptPasswordAndAdminRole() {
		Role admin = new Role();
		admin.setId(1L);
		admin.setName("ADMIN");
		when(userRepository.existsByRolesName("ADMIN")).thenReturn(false);
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
		when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin));

		runner(enabled()).run(arguments);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertEquals("admin@example.com", saved.getEmail());
		assertEquals("Ada", saved.getFirstName());
		assertEquals("Admin", saved.getLastName());
		assertEquals(1, saved.getRoles().size());
		assertTrue(saved.getRoles().contains(admin));
		assertTrue(new BCryptPasswordEncoder().matches("strong-password", saved.getPassword()));
		assertTrue(saved.getPassword().startsWith("$2"));
	}

	@Test
	void existingAdministratorPreventsDuplicate() {
		when(userRepository.existsByRolesName("ADMIN")).thenReturn(true);
		runner(enabled()).run(arguments);
		verify(userRepository, never()).save(any());
		verify(roleRepository, never()).findByName(any());
	}

	@Test
	void existingEmailPreventsDuplicateEvenWithoutAdminRole() {
		when(userRepository.existsByRolesName("ADMIN")).thenReturn(false);
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);
		runner(enabled()).run(arguments);
		verify(userRepository, never()).save(any());
	}

	@Test
	void enabledRequiresAllValuesAndStrongEnoughPassword() {
		var properties = new BootstrapAdminProperties(true, "admin@example.com", "short", "Ada", "Admin");
		org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
				() -> runner(properties).run(arguments));
		verify(userRepository, never()).save(any());
	}

	private BootstrapAdminRunner runner(BootstrapAdminProperties properties) {
		return new BootstrapAdminRunner(properties, userRepository, roleRepository,
				new BCryptPasswordEncoder());
	}

	private BootstrapAdminProperties enabled() {
		return new BootstrapAdminProperties(true, " Admin@Example.COM ", "strong-password", " Ada ", " Admin ");
	}
}
