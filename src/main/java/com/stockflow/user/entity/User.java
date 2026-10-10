package com.stockflow.user.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.stockflow.role.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@NotBlank
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@NotBlank
	@Column(nullable = false, length = 255)
	private String email;

	@NotBlank
	@Column(nullable = false, length = 255)
	private String password;

	@NotNull
	@Column(nullable = false)
	private Boolean active = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id", nullable = false),
			inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false),
			uniqueConstraints = @UniqueConstraint(
					name = "uk_user_roles",
					columnNames = { "user_id", "role_id" }
			)
	)
	private Set<Role> roles = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	private void onCreate() {
		Instant now = Instant.now();
		if (active == null) {
			active = true;
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = Instant.now();
	}
}
