package com.stockflow.auth.service;

import java.util.Locale;

import com.stockflow.auth.dto.LoginRequest;
import com.stockflow.auth.dto.LoginResponse;
import com.stockflow.security.JwtService;
import com.stockflow.user.entity.User;
import com.stockflow.user.mapper.UserMapper;
import com.stockflow.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final JwtService jwtService;

	public AuthService(
			AuthenticationManager authenticationManager,
			UserRepository userRepository,
			UserMapper userMapper,
			JwtService jwtService
	) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							email, request.password()
					)
			);
		} catch (AuthenticationException exception) {
			throw new BadCredentialsException("Invalid credentials");
		}

		UserDetails principal = (UserDetails) authentication.getPrincipal();
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
		if (!Boolean.TRUE.equals(user.getActive()) || !principal.isEnabled()) {
			throw new BadCredentialsException("Invalid credentials");
		}

		return new LoginResponse(
				jwtService.generateToken(principal),
				"Bearer",
				jwtService.getExpirationSeconds(),
				userMapper.toResponse(user)
		);
	}
}
