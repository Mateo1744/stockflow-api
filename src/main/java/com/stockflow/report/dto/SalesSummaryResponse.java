package com.stockflow.report.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(long totalSales, BigDecimal totalRevenue,
		long completedSales, long cancelledSales, long pendingSales) {
}
