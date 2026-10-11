package com.stockflow.supplier.repository;

import java.util.Optional;

import com.stockflow.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

	Optional<Supplier> findByEmail(String email);

	boolean existsByEmail(String email);
}
