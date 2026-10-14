package com.stockflow.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record StockFlowUserPrincipal(
		Long id,
		String username,
		String password,
		boolean enabled,
		List<GrantedAuthority> authorities
) implements UserDetails {

	public StockFlowUserPrincipal {
		authorities = List.copyOf(authorities);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
