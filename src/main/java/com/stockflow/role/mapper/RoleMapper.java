package com.stockflow.role.mapper;

import com.stockflow.role.dto.RoleResponse;
import com.stockflow.role.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

	public RoleResponse toResponse(Role role) {
		return new RoleResponse(
				role.getId(),
				role.getName(),
				role.getDescription()
		);
	}
}
