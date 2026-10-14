package com.stockflow.supplier.controller;

import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.supplier.dto.SupplierRequest;
import com.stockflow.supplier.dto.SupplierResponse;
import com.stockflow.supplier.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

	@Mock
	private SupplierService supplierService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new SupplierController(supplierService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(supplierService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/suppliers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].taxId").value("900123"));
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(supplierService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/suppliers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		when(supplierService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Supplier not found"));

		mockMvc.perform(get("/api/suppliers/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createReturnsCreated() throws Exception {
		when(supplierService.create(any(SupplierRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/suppliers")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Supplier"));
	}

	@Test
	void createReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/suppliers")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\",\"taxId\":\"\",\"email\":\"bad\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.taxId").exists())
				.andExpect(jsonPath("$.fieldErrors.email").exists());
	}

	@Test
	void createReturnsConflictForDuplicate() throws Exception {
		when(supplierService.create(any(SupplierRequest.class)))
				.thenThrow(new DuplicateResourceException("Supplier already exists"));

		mockMvc.perform(post("/api/suppliers")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isConflict());
	}

	@Test
	void updateReturnsOk() throws Exception {
		when(supplierService.update(any(Long.class), any(SupplierRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/suppliers/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isOk());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/suppliers/1"))
				.andExpect(status().isNoContent());

		verify(supplierService).delete(1L);
	}

	private String validJson() {
		return "{\"name\":\"Supplier\",\"taxId\":\"900123\","
				+ "\"email\":\"supplier@test.com\",\"active\":true}";
	}

	private SupplierResponse response() {
		return new SupplierResponse(
				1L, "Supplier", "900123", "Contact", "supplier@test.com",
				"123", "Address", true, null, null
		);
	}
}
