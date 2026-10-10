package com.stockflow.inventory.entity;

import java.time.Instant;

import com.stockflow.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, unique = true)
	private Product product;

	@NotNull
	@PositiveOrZero
	@Column(name = "current_stock", nullable = false)
	private Integer currentStock = 0;

	@NotNull
	@PositiveOrZero
	@Column(name = "minimum_stock", nullable = false)
	private Integer minimumStock = 0;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	private void onCreate() {
		if (currentStock == null) {
			currentStock = 0;
		}
		if (minimumStock == null) {
			minimumStock = 0;
		}
		updatedAt = Instant.now();
	}

	@PreUpdate
	private void onUpdate() {
		updatedAt = Instant.now();
	}
}
