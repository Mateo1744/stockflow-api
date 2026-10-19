package com.stockflow.sale.repository;

import java.util.Optional;
import java.time.Instant;

import com.stockflow.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

	Optional<Sale> findBySaleNumber(String saleNumber);

	boolean existsBySaleNumber(String saleNumber);

	@Query("""
			select count(s),
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then s.totalAmount else 0 end), 0),
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then 1 else 0 end), 0),
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.CANCELLED then 1 else 0 end), 0),
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.PENDING then 1 else 0 end), 0)
			from Sale s
			where (:from is null or s.createdAt >= :from) and (:to is null or s.createdAt <= :to)
			""")
	Object[] summarize(@Param("from") Instant from, @Param("to") Instant to);
}
