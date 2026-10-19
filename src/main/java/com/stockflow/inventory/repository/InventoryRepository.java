package com.stockflow.inventory.repository;

import java.util.Optional;

import com.stockflow.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

	Optional<Inventory> findByProductId(Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inventory i where i.product.id = :productId")
	Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

	@Query("select i from Inventory i join fetch i.product p where i.currentStock <= i.minimumStock order by p.id")
	java.util.List<Inventory> findLowStock();

	@Query("select coalesce(sum(i.currentStock), 0) from Inventory i")
	Long sumCurrentStock();

	@Query("select count(i) from Inventory i where i.currentStock <= i.minimumStock")
	long countLowStock();
}
