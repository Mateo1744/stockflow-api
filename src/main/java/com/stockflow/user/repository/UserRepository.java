package com.stockflow.user.repository;

import java.util.Optional;

import com.stockflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByRolesName(String roleName);
}
