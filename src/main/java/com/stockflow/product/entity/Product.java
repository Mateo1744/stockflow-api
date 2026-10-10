package com.stockflow.product.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.stockflow.category.entity.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "products",
		uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
)
@Getter
@Setter
@NoArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false, length = 100)
	private String sku;

	@NotBlank
	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 500)
	private String description;

	@NotNull
	@PositiveOrZero
	@Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal salePrice;

	@NotNull
	@Column(nullable = false)
	private Boolean active = true;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

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
