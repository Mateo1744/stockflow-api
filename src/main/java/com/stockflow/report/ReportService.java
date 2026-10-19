package com.stockflow.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.report.dto.InventorySummaryResponse;
import com.stockflow.report.dto.LowStockResponse;
import com.stockflow.report.dto.SalesSummaryResponse;
import com.stockflow.sale.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
	private final InventoryRepository inventoryRepository;
	private final SaleRepository saleRepository;

	public ReportService(InventoryRepository inventoryRepository, SaleRepository saleRepository) {
		this.inventoryRepository = inventoryRepository;
		this.saleRepository = saleRepository;
	}

	@Transactional(readOnly = true)
	public List<LowStockResponse> lowStock() {
		return inventoryRepository.findLowStock().stream().map(inventory -> new LowStockResponse(
				inventory.getProduct().getId(), inventory.getProduct().getSku(),
				inventory.getProduct().getName(), inventory.getCurrentStock(), inventory.getMinimumStock()))
				.toList();
	}

	@Transactional(readOnly = true)
	public SalesSummaryResponse salesSummary(Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException("from must be before or equal to to");
		}
		Object[] values = saleRepository.summarize(from, to);
		return new SalesSummaryResponse(number(values[0]).longValue(), decimal(values[1]),
				number(values[2]).longValue(), number(values[3]).longValue(), number(values[4]).longValue());
	}

	@Transactional(readOnly = true)
	public InventorySummaryResponse inventorySummary() {
		return new InventorySummaryResponse(inventoryRepository.count(),
				inventoryRepository.sumCurrentStock(), inventoryRepository.countLowStock());
	}

	private Number number(Object value) { return value == null ? 0L : (Number) value; }
	private BigDecimal decimal(Object value) {
		return value == null ? BigDecimal.ZERO : value instanceof BigDecimal decimal
				? decimal : new BigDecimal(value.toString());
	}
}
