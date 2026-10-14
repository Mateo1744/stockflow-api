package com.stockflow.security;

import java.util.List;

import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockFlowUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public StockFlowUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

		List<GrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
				.distinct()
				.sorted((left, right) -> left.getAuthority().compareTo(right.getAuthority()))
				.map(GrantedAuthority.class::cast)
				.toList();

		return new StockFlowUserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPassword(),
				Boolean.TRUE.equals(user.getActive()),
				authorities
		);
	}
}
