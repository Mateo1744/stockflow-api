package com.stockflow.sale.repository;

import java.math.BigDecimal;

public interface SalesSummaryProjection {
	Long getTotalSales();
	BigDecimal getTotalRevenue();
	Long getCompletedSales();
	Long getCancelledSales();
	Long getPendingSales();
}
