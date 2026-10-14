package com.stockflow.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.role.entity.Role;
import com.stockflow.role.repository.RoleRepository;
import com.stockflow.user.dto.UserCreateRequest;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.dto.UserUpdateRequest;
import com.stockflow.user.entity.User;
import com.stockflow.user.mapper.UserMapper;
import com.stockflow.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private static final Set<String> SYSTEM_ROLES = Set.of(
			"ADMIN", "INVENTORY_MANAGER", "SELLER"
	);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserMapper userMapper,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userRepository.findAll().stream()
				.map(userMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public UserResponse findById(Long id) {
		return userMapper.toResponse(findEntityById(id));
	}

	@Transactional(readOnly = true)
	public UserResponse findByEmail(String email) {
		return userMapper.toResponse(findEntityByEmail(normalizeEmail(email)));
	}

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateResourceException("User already exists with email: " + email);
		}

		User user = new User();
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setActive(request.active());
		user.setRoles(resolveRoles(request.roleNames()));

		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(Long id, UserUpdateRequest request) {
		User user = findEntityById(id);
		String email = normalizeEmail(request.email());

		if (!user.getEmail().equalsIgnoreCase(email)
				&& userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateResourceException("User already exists with email: " + email);
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(email);
		user.setActive(request.active());
		user.setRoles(resolveRoles(request.roleNames()));
		return userMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public void delete(Long id) {
		User user = findEntityById(id);
		user.setActive(false);
		userRepository.save(user);
	}

	private User findEntityById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"User not found with id: " + id
				));
	}

	private User findEntityByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResourceNotFoundException(
						"User not found with email: " + email
				));
	}

	private Set<Role> resolveRoles(Set<String> requestedRoleNames) {
		if (requestedRoleNames == null || requestedRoleNames.isEmpty()) {
			throw new InvalidOperationException("A user must have at least one role");
		}

		Set<Role> roles = new HashSet<>();
		for (String requestedRoleName : requestedRoleNames) {
			if (requestedRoleName == null || requestedRoleName.isBlank()) {
				throw new InvalidOperationException("Role names must not be blank");
			}
			String roleName = requestedRoleName.trim().toUpperCase(Locale.ROOT);
			if (!SYSTEM_ROLES.contains(roleName)) {
				throw new ResourceNotFoundException("Role not found with name: " + roleName);
			}
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new ResourceNotFoundException(
							"Role not found with name: " + roleName
					));
			roles.add(role);
		}
		return roles;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
