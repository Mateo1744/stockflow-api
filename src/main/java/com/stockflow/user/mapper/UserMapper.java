package com.stockflow.user.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import com.stockflow.role.dto.RoleResponse;
import com.stockflow.role.mapper.RoleMapper;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	private final RoleMapper roleMapper;

	public UserMapper(RoleMapper roleMapper) {
		this.roleMapper = roleMapper;
	}

	public UserResponse toResponse(User user) {
		Set<RoleResponse> roles = user.getRoles().stream()
				.map(roleMapper::toResponse)
				.collect(Collectors.toUnmodifiableSet());

		return new UserResponse(
				user.getId(),
				user.getFirstName(),
				user.getLastName(),
				user.getEmail(),
				user.getActive(),
				roles,
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
