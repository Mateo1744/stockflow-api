package com.stockflow.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.product.entity.Product;
import com.stockflow.report.dto.SalesSummaryResponse;
import com.stockflow.sale.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
	@Mock InventoryRepository inventoryRepository;
	@Mock SaleRepository saleRepository;
	@InjectMocks ReportService reportService;

	@Test
	void lowStockUsesRepositoryQueryAndMapsOnlyItsResults() {
		Inventory low = inventory(1L, "SKU-1", 5, 5);
		when(inventoryRepository.findLowStock()).thenReturn(List.of(low));
		assertEquals(List.of(1L), reportService.lowStock().stream().map(r -> r.productId()).toList());
	}

	@Test
	void salesSummaryMapsTotalsAndMoneyExactly() {
		when(saleRepository.summarize(null, null)).thenReturn(
				new Object[]{5L, new BigDecimal("125.40"), 3L, 1L, 1L});
		SalesSummaryResponse result = reportService.salesSummary(null, null);
		assertEquals(5, result.totalSales());
		assertEquals(new BigDecimal("125.40"), result.totalRevenue());
		assertEquals(3, result.completedSales());
		assertEquals(1, result.cancelledSales());
		assertEquals(1, result.pendingSales());
	}

	@Test
	void salesSummaryRejectsReversedRange() {
		assertThrows(IllegalArgumentException.class, () -> reportService.salesSummary(
				Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")));
	}

	@Test
	void inventorySummaryReturnsProductsUnitsAndLowStockCount() {
		when(inventoryRepository.count()).thenReturn(8L);
		when(inventoryRepository.sumCurrentStock()).thenReturn(42L);
		when(inventoryRepository.countLowStock()).thenReturn(2L);
		var result = reportService.inventorySummary();
		assertEquals(8, result.totalProducts());
		assertEquals(42, result.totalUnitsInStock());
		assertEquals(2, result.lowStockProducts());
	}

	private Inventory inventory(Long id, String sku, int stock, int minimum) {
		Product product = new Product(); product.setId(id); product.setSku(sku); product.setName("Product " + sku);
		Inventory inventory = new Inventory(); inventory.setProduct(product);
		inventory.setCurrentStock(stock); inventory.setMinimumStock(minimum); return inventory;
	}
}
