package com.stockflow.common.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagedListingsPostgresIntegrationTest {

	@Autowired MockMvc mockMvc;
	@Autowired JdbcTemplate jdbcTemplate;

	@BeforeEach
	void emptyListedResources() {
		for (String table : new String[]{"sale_items", "stock_entry_items", "stock_exit_items",
				"inventory_movements", "sales", "stock_entries", "stock_exits", "inventory",
				"products", "customers", "suppliers", "categories"}) {
			jdbcTemplate.update("delete from " + table);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/api/products?page=0&size=10",
			"/api/products?page=0&size=10&sort=id,asc",
			"/api/customers?page=0&size=10",
			"/api/customers?page=0&size=10&sort=id,asc",
			"/api/suppliers?page=0&size=10",
			"/api/suppliers?page=0&size=10&sort=id,asc",
			"/api/inventory-movements?page=0&size=10",
			"/api/inventory-movements?page=0&size=10&sort=id,desc",
			"/api/stock-entries?page=0&size=10",
			"/api/stock-entries?page=0&size=10&sort=id,desc",
			"/api/stock-exits?page=0&size=10",
			"/api/stock-exits?page=0&size=10&sort=id,desc",
			"/api/sales?page=0&size=10",
			"/api/sales?page=0&size=10&sort=id,desc"
	})
	void emptyListingAcceptsPaginationAndFrontendSort(String path) throws Exception {
		mockMvc.perform(get(path).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(0))
				.andExpect(jsonPath("$.totalPages").value(0))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void invalidSortFieldReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/products?page=0&size=10&sort=notAField,asc")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").isNotEmpty());
	}
}
