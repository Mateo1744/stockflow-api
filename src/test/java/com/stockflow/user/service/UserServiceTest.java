package com.stockflow.user.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.role.entity.Role;
import com.stockflow.role.repository.RoleRepository;
import com.stockflow.user.dto.UserCreateRequest;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.dto.UserUpdateRequest;
import com.stockflow.user.entity.User;
import com.stockflow.user.mapper.UserMapper;
import com.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(
				userRepository, roleRepository, userMapper, passwordEncoder
		);
	}

	@Test
	void findAllMapsUsers() {
		User user = user();
		UserResponse response = response();
		when(userRepository.findAll()).thenReturn(List.of(user));
		when(userMapper.toResponse(user)).thenReturn(response);

		assertEquals(List.of(response), userService.findAll());
	}

	@Test
	void findByIdReturnsUser() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userMapper.toResponse(user)).thenReturn(response());

		assertEquals(1L, userService.findById(1L).id());
	}

	@Test
	void findByIdRejectsMissingUser() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
	}

	@Test
	void createPersistsResolvedRolesAndNormalizedEmail() {
		Role admin = role("ADMIN");
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
		when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin));
		when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.toResponse(any(User.class))).thenReturn(response());

		userService.create(createRequest(Set.of("admin")));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();
		assertEquals("admin@example.com", saved.getEmail());
		assertEquals(Set.of(admin), saved.getRoles());
		assertEquals("bcrypt-hash", saved.getPassword());
		assertEquals(true, saved.getActive());
	}

	@Test
	void createRejectsDuplicateEmail() {
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

		assertThrows(
				DuplicateResourceException.class,
				() -> userService.create(createRequest(Set.of("ADMIN")))
		);
		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void createRejectsMissingRole() {
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
		when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

		assertThrows(
				ResourceNotFoundException.class,
				() -> userService.create(createRequest(Set.of("ADMIN")))
		);
		verify(userRepository, never()).save(any());
	}

	@Test
	void createEncodesPasswordInsteadOfPersistingRawValue() {
		Role seller = role("SELLER");
		when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
		when(roleRepository.findByName("SELLER")).thenReturn(Optional.of(seller));
		when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.toResponse(any(User.class))).thenReturn(response());

		userService.create(createRequest(Set.of("SELLER")));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertEquals("encoded-password", captor.getValue().getPassword());
		verify(passwordEncoder).encode("secret123");
	}

	@Test
	void updateChangesAllowedFieldsAndRoles() {
		User user = user();
		Role manager = role("INVENTORY_MANAGER");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(false);
		when(roleRepository.findByName("INVENTORY_MANAGER")).thenReturn(Optional.of(manager));
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toResponse(user)).thenReturn(response());

		userService.update(1L, updateRequest("updated@example.com", Set.of("INVENTORY_MANAGER")));

		assertEquals("Updated", user.getFirstName());
		assertEquals("updated@example.com", user.getEmail());
		assertEquals(Set.of(manager), user.getRoles());
	}

	@Test
	void updateRejectsEmailConflict() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(true);

		assertThrows(
				DuplicateResourceException.class,
				() -> userService.update(
						1L, updateRequest("updated@example.com", Set.of("ADMIN"))
				)
		);
		verify(userRepository, never()).save(any());
	}

	@Test
	void updateDoesNotOverwritePassword() {
		User user = user();
		String originalPassword = user.getPassword();
		Role admin = role("ADMIN");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin));
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toResponse(user)).thenReturn(response());

		userService.update(1L, updateRequest("admin@example.com", Set.of("ADMIN")));

		assertEquals(originalPassword, user.getPassword());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void deletePerformsSoftDelete() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		userService.delete(1L);

		assertFalse(user.getActive());
		verify(userRepository).save(user);
	}

	@Test
	void findByEmailUsesCaseInsensitiveLookup() {
		User user = user();
		when(userRepository.findByEmailIgnoreCase("admin@example.com"))
				.thenReturn(Optional.of(user));
		when(userMapper.toResponse(user)).thenReturn(response());

		assertEquals(response(), userService.findByEmail(" ADMIN@EXAMPLE.COM "));
	}

	private UserCreateRequest createRequest(Set<String> roles) {
		return new UserCreateRequest(
				"Admin", "User", " Admin@Example.com ", "secret123", true, roles
		);
	}

	private UserUpdateRequest updateRequest(String email, Set<String> roles) {
		return new UserUpdateRequest("Updated", "User", email, true, roles);
	}

	private User user() {
		User user = new User();
		user.setId(1L);
		user.setFirstName("Admin");
		user.setLastName("User");
		user.setEmail("admin@example.com");
		user.setPassword("existing-hash");
		user.setActive(true);
		return user;
	}

	private Role role(String name) {
		Role role = new Role();
		role.setId(1L);
		role.setName(name);
		return role;
	}

	private UserResponse response() {
		return new UserResponse(1L, "Admin", "User", "admin@example.com", true,
				Set.of(), null, null);
	}
}
