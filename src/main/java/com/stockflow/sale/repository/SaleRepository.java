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
			select count(s) as totalSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then s.totalAmount else 0 end), 0) as totalRevenue,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then 1 else 0 end), 0) as completedSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.CANCELLED then 1 else 0 end), 0) as cancelledSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.PENDING then 1 else 0 end), 0) as pendingSales
			from Sale s
			""")
	SalesSummaryProjection summarize();

	@Query("""
			select count(s) as totalSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then s.totalAmount else 0 end), 0) as totalRevenue,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then 1 else 0 end), 0) as completedSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.CANCELLED then 1 else 0 end), 0) as cancelledSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.PENDING then 1 else 0 end), 0) as pendingSales
			from Sale s
			where s.createdAt >= :from
			""")
	SalesSummaryProjection summarizeFrom(@Param("from") Instant from);

	@Query("""
			select count(s) as totalSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then s.totalAmount else 0 end), 0) as totalRevenue,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then 1 else 0 end), 0) as completedSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.CANCELLED then 1 else 0 end), 0) as cancelledSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.PENDING then 1 else 0 end), 0) as pendingSales
			from Sale s
			where s.createdAt <= :to
			""")
	SalesSummaryProjection summarizeTo(@Param("to") Instant to);

	@Query("""
			select count(s) as totalSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then s.totalAmount else 0 end), 0) as totalRevenue,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.COMPLETED then 1 else 0 end), 0) as completedSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.CANCELLED then 1 else 0 end), 0) as cancelledSales,
			coalesce(sum(case when s.status = com.stockflow.sale.entity.Sale.Status.PENDING then 1 else 0 end), 0) as pendingSales
			from Sale s
			where s.createdAt >= :from and s.createdAt <= :to
			""")
	SalesSummaryProjection summarizeBetween(@Param("from") Instant from, @Param("to") Instant to);
}
