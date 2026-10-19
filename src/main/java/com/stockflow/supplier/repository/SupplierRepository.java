package com.stockflow.supplier.repository;

import java.util.Optional;

import com.stockflow.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

	Optional<Supplier> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByTaxId(String taxId);
}
