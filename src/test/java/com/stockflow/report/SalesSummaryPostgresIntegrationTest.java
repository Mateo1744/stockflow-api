package com.stockflow.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.stockflow.report.dto.SalesSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SalesSummaryPostgresIntegrationTest {

	@Autowired ReportService reportService;
	@Autowired JdbcTemplate jdbcTemplate;

	private long sellerId;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("delete from sale_items");
		jdbcTemplate.update("delete from sales");
		sellerId = jdbcTemplate.queryForObject("""
				insert into users (first_name, last_name, email, password)
				values ('Report', 'Tester', ?, 'not-used') returning id
				""", Long.class, "reports-" + UUID.randomUUID() + "@example.com");
	}

	@Test
	void noDatesWithNoSalesReturnsZeroSummaryOnPostgres() {
		assertSummary(reportService.salesSummary(null, null), 0, "0", 0, 0, 0);
	}

	@Test
	void noDatesWithSalesSummarizesAllSales() {
		insertSale("S-ALL-1", "COMPLETED", "10.25", "2026-01-10T12:00:00Z");
		insertSale("S-ALL-2", "CANCELLED", "99.00", "2026-02-10T12:00:00Z");
		insertSale("S-ALL-3", "PENDING", "5.00", "2026-03-10T12:00:00Z");
		assertSummary(reportService.salesSummary(null, null), 3, "10.25", 1, 1, 1);
	}

	@Test
	void fromOnlyUsesInclusiveLowerBound() {
		insertSale("S-FROM-1", "COMPLETED", "10.00", "2026-01-01T00:00:00Z");
		insertSale("S-FROM-2", "COMPLETED", "20.00", "2026-02-01T00:00:00Z");
		assertSummary(reportService.salesSummary(Instant.parse("2026-02-01T00:00:00Z"), null),
				1, "20.00", 1, 0, 0);
	}

	@Test
	void toOnlyUsesInclusiveUpperBound() {
		insertSale("S-TO-1", "PENDING", "10.00", "2026-02-01T00:00:00Z");
		insertSale("S-TO-2", "CANCELLED", "20.00", "2026-02-02T00:00:00Z");
		assertSummary(reportService.salesSummary(null, Instant.parse("2026-02-01T00:00:00Z")),
				1, "0", 0, 0, 1);
	}

	@Test
	void fromAndToUseInclusiveRange() {
		insertSale("S-RANGE-1", "COMPLETED", "1.00", "2026-01-31T23:59:59Z");
		insertSale("S-RANGE-2", "COMPLETED", "2.50", "2026-02-01T00:00:00Z");
		insertSale("S-RANGE-3", "CANCELLED", "3.00", "2026-02-28T23:59:59Z");
		insertSale("S-RANGE-4", "PENDING", "4.00", "2026-03-01T00:00:00Z");
		assertSummary(reportService.salesSummary(Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-02-28T23:59:59Z")), 2, "2.50", 1, 1, 0);
	}

	private void insertSale(String number, String status, String amount, String createdAt) {
		jdbcTemplate.update("""
				insert into sales (sale_number, status, seller_id, total_amount, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?)
				""", number, status, sellerId, new BigDecimal(amount),
				Timestamp.from(Instant.parse(createdAt)), Timestamp.from(Instant.parse(createdAt)));
	}

	private void assertSummary(SalesSummaryResponse actual, long total, String revenue,
			long completed, long cancelled, long pending) {
		assertEquals(total, actual.totalSales());
		assertEquals(0, new BigDecimal(revenue).compareTo(actual.totalRevenue()));
		assertEquals(completed, actual.completedSales());
		assertEquals(cancelled, actual.cancelledSales());
		assertEquals(pending, actual.pendingSales());
	}
}
