package com.stockflow.report;

import java.time.Instant;
import java.util.List;

import com.stockflow.report.dto.InventorySummaryResponse;
import com.stockflow.report.dto.LowStockResponse;
import com.stockflow.report.dto.SalesSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Operational inventory and sales reports")
public class ReportController {
	private final ReportService reportService;
	public ReportController(ReportService reportService) { this.reportService = reportService; }

	@GetMapping("/low-stock")
	@Operation(summary = "List products at or below their minimum stock")
	public ResponseEntity<List<LowStockResponse>> lowStock() {
		return ResponseEntity.ok(reportService.lowStock());
	}

	@GetMapping("/sales-summary")
	@Operation(summary = "Summarize sales", description = "Optional from/to parameters are inclusive ISO-8601 instants.")
	public ResponseEntity<SalesSummaryResponse> salesSummary(
			@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
		return ResponseEntity.ok(reportService.salesSummary(from, to));
	}

	@GetMapping("/inventory-summary")
	@Operation(summary = "Summarize inventory quantities")
	public ResponseEntity<InventorySummaryResponse> inventorySummary() {
		return ResponseEntity.ok(reportService.inventorySummary());
	}
}
