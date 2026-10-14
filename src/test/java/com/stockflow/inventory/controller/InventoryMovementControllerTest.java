package com.stockflow.inventory.controller;

import java.util.List;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.service.InventoryMovementService;
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
class InventoryMovementControllerTest {

	@Mock
	private InventoryMovementService inventoryMovementService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new InventoryMovementController(inventoryMovementService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturns200() throws Exception {
		when(inventoryMovementService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/inventory-movements"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].type").value("EXIT"));
	}

	@Test
	void findByIdReturns200() throws Exception {
		when(inventoryMovementService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/inventory-movements/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturns404WhenMissing() throws Exception {
		when(inventoryMovementService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Missing"));

		mockMvc.perform(get("/api/inventory-movements/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mutableEndpointsDoNotExist() throws Exception {
		mockMvc.perform(post("/api/inventory-movements"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/inventory-movements/1"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(patch("/api/inventory-movements/1"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/inventory-movements/1"))
				.andExpect(status().isMethodNotAllowed());
	}

	private InventoryMovementResponse response() {
		return new InventoryMovementResponse(
				1L, 2L, 3L, "SKU-1", "Keyboard", InventoryMovement.Type.EXIT,
				2, 10, 8, 7L, "Ana", "Diaz", "ana@example.com", "Sale X", null
		);
	}
}
