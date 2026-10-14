package com.stockflow.security;

import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public String getCurrentEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		return authentication.getName();
	}

	@Transactional(readOnly = true)
	public User getCurrentUser() {
		String email = getCurrentEmail();
		return userRepository.findByEmailIgnoreCase(email)
				.filter(user -> Boolean.TRUE.equals(user.getActive()))
				.orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
						"Authentication is required"
				));
	}
}
