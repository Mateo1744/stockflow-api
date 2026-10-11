package com.stockflow.stockentry.repository;

import java.util.Optional;

import com.stockflow.stockentry.entity.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

	Optional<StockEntry> findBySupplierIdAndReferenceNumber(Long supplierId, String referenceNumber);

	boolean existsBySupplierIdAndReferenceNumber(Long supplierId, String referenceNumber);
}
