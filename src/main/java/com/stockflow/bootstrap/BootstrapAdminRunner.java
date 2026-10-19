package com.stockflow.bootstrap;

import java.util.Locale;
import java.util.Set;

import com.stockflow.role.entity.Role;
import com.stockflow.role.repository.RoleRepository;
import com.stockflow.user.entity.User;
import com.stockflow.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class BootstrapAdminRunner implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminRunner.class);
	private static final String ADMIN_ROLE = "ADMIN";

	private final BootstrapAdminProperties properties;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public BootstrapAdminRunner(BootstrapAdminProperties properties, UserRepository userRepository,
			RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.enabled()) {
			return;
		}
		validateConfiguration();
		if (userRepository.existsByRolesName(ADMIN_ROLE)) {
			LOGGER.info("Bootstrap ADMIN skipped because an administrator already exists");
			return;
		}
		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(email)) {
			LOGGER.info("Bootstrap ADMIN skipped because the configured email already exists");
			return;
		}
		Role admin = roleRepository.findByName(ADMIN_ROLE)
				.orElseThrow(() -> new IllegalStateException("ADMIN role is missing; verify Flyway V2"));
		User user = new User();
		user.setEmail(email);
		user.setFirstName(properties.firstName().trim());
		user.setLastName(properties.lastName().trim());
		user.setPassword(passwordEncoder.encode(properties.password()));
		user.setActive(true);
		user.setRoles(Set.of(admin));
		userRepository.save(user);
		LOGGER.info("Bootstrap ADMIN created for configured email");
	}

	private void validateConfiguration() {
		if (isBlank(properties.email()) || isBlank(properties.password())
				|| isBlank(properties.firstName()) || isBlank(properties.lastName())) {
			throw new IllegalStateException(
					"All STOCKFLOW_BOOTSTRAP_ADMIN_* values are required when bootstrap is enabled");
		}
		if (properties.password().length() < 8) {
			throw new IllegalStateException("Bootstrap ADMIN password must contain at least 8 characters");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
