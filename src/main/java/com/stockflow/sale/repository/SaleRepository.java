package com.stockflow.sale.repository;

import java.util.Optional;

import com.stockflow.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

	Optional<Sale> findBySaleNumber(String saleNumber);

	boolean existsBySaleNumber(String saleNumber);
}
