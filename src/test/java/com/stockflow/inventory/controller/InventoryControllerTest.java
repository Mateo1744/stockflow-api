package com.stockflow.inventory.controller;

import java.util.List;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.inventory.dto.InventoryResponse;
import com.stockflow.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

	@Mock
	private InventoryService inventoryService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new InventoryController(inventoryService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(inventoryService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/inventory"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].currentStock").value(12));
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(inventoryService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/inventory/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByProductIdReturnsOk() throws Exception {
		when(inventoryService.findByProductId(10L)).thenReturn(response());

		mockMvc.perform(get("/api/inventory/product/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(10));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		when(inventoryService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Inventory not found"));

		mockMvc.perform(get("/api/inventory/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void findByProductIdReturnsNotFound() throws Exception {
		when(inventoryService.findByProductId(99L))
				.thenThrow(new ResourceNotFoundException("Inventory not found"));

		mockMvc.perform(get("/api/inventory/product/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mutableEndpointsDoNotExist() throws Exception {
		mockMvc.perform(post("/api/inventory")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/inventory/1")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(patch("/api/inventory/1")).andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/inventory/1")).andExpect(status().isMethodNotAllowed());
	}

	private InventoryResponse response() {
		return new InventoryResponse(1L, 10L, "SKU-1", "Keyboard", 12, 2, null);
	}
}
