package com.stockflow.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockflow.sale.entity.Sale;

public record SaleResponse(
		Long id,
		String saleNumber,
		Sale.Status status,
		Long customerId,
		String customerDocumentNumber,
		String customerFirstName,
		String customerLastName,
		String customerEmail,
		Long sellerId,
		String sellerFirstName,
		String sellerLastName,
		String sellerEmail,
		BigDecimal totalAmount,
		List<SaleItemResponse> items,
		Instant createdAt,
		Instant updatedAt
) {
}
