package com.stockflow.report;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.math.BigDecimal;
import java.util.List;

import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.report.dto.InventorySummaryResponse;
import com.stockflow.report.dto.LowStockResponse;
import com.stockflow.report.dto.SalesSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {
	@Mock ReportService reportService;
	MockMvc mockMvc;

	@BeforeEach void setUp() {
		mockMvc = standaloneSetup(new ReportController(reportService))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test void lowStockReturnsOk() throws Exception {
		when(reportService.lowStock()).thenReturn(List.of(new LowStockResponse(1L, "SKU", "Name", 2, 3)));
		mockMvc.perform(get("/api/reports/low-stock")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].sku").value("SKU"));
	}

	@Test void salesSummaryReturnsOk() throws Exception {
		when(reportService.salesSummary(null, null)).thenReturn(new SalesSummaryResponse(2,
				new BigDecimal("10.50"), 1, 0, 1));
		mockMvc.perform(get("/api/reports/sales-summary")).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRevenue").value(10.50));
	}

	@Test void inventorySummaryReturnsOk() throws Exception {
		when(reportService.inventorySummary()).thenReturn(new InventorySummaryResponse(5, 20, 2));
		mockMvc.perform(get("/api/reports/inventory-summary")).andExpect(status().isOk())
				.andExpect(jsonPath("$.lowStockProducts").value(2));
	}
}
