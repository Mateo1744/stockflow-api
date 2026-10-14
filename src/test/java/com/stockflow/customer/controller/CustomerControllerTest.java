package com.stockflow.customer.controller;

import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.customer.dto.CustomerRequest;
import com.stockflow.customer.dto.CustomerResponse;
import com.stockflow.customer.service.CustomerService;
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
class CustomerControllerTest {

	@Mock
	private CustomerService customerService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new CustomerController(customerService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(customerService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/customers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].documentNumber").value("123456"));
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(customerService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/customers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		when(customerService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Customer not found"));

		mockMvc.perform(get("/api/customers/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createReturnsCreated() throws Exception {
		when(customerService.create(any(CustomerRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/customers")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Mateo"));
	}

	@Test
	void createReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/customers")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"documentNumber\":\"\",\"firstName\":\"\","
							+ "\"lastName\":\"\",\"email\":\"bad\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.documentNumber").exists())
				.andExpect(jsonPath("$.fieldErrors.firstName").exists())
				.andExpect(jsonPath("$.fieldErrors.lastName").exists());
	}

	@Test
	void createReturnsConflictForDuplicate() throws Exception {
		when(customerService.create(any(CustomerRequest.class)))
				.thenThrow(new DuplicateResourceException("Customer already exists"));

		mockMvc.perform(post("/api/customers")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isConflict());
	}

	@Test
	void updateReturnsOk() throws Exception {
		when(customerService.update(any(Long.class), any(CustomerRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/customers/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isOk());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/customers/1"))
				.andExpect(status().isNoContent());

		verify(customerService).delete(1L);
	}

	private String validJson() {
		return "{\"documentNumber\":\"123456\",\"firstName\":\"Mateo\","
				+ "\"lastName\":\"Test\",\"email\":\"mateo@test.com\","
				+ "\"active\":true}";
	}

	private CustomerResponse response() {
		return new CustomerResponse(
				1L, "123456", "Mateo", "Test", "mateo@test.com", "123",
				"Address", true, null, null
		);
	}
}
