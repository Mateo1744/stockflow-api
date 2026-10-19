package com.stockflow.product.controller;

import java.math.BigDecimal;
import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import com.stockflow.product.service.ProductService;
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
class ProductControllerTest {

	@Mock
	private ProductService productService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new ProductController(productService))
				.setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(productService.findAll(any(), any(), any(), any(), any())).thenReturn(
				new com.stockflow.common.dto.PageResponse<>(List.of(response()), 0, 20, 1, 1, true, true));

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].sku").value("SKU-1"));
	}

	@Test
	void findAllAcceptsCombinedFiltersPaginationAndSort() throws Exception {
		when(productService.findAll(any(), any(), any(), any(), any())).thenReturn(
				new com.stockflow.common.dto.PageResponse<>(List.of(), 1, 5, 0, 0, false, true));
		mockMvc.perform(get("/api/products").param("name", "mouse").param("active", "true")
				.param("page", "1").param("size", "5").param("sort", "name,desc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(5));
		verify(productService).findAll(any(), org.mockito.ArgumentMatchers.eq("mouse"),
				org.mockito.ArgumentMatchers.eq(true), any(), any());
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(productService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		when(productService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Product not found"));

		mockMvc.perform(get("/api/products/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void createReturnsCreated() throws Exception {
		when(productService.create(any(ProductRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sku").value("SKU-1"));
	}

	@Test
	void createReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"sku\":\"\",\"name\":\"\",\"salePrice\":-1,"
							+ "\"active\":true,\"categoryId\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.sku").exists())
				.andExpect(jsonPath("$.fieldErrors.salePrice").exists())
				.andExpect(jsonPath("$.fieldErrors.categoryId").exists());
	}

	@Test
	void createReturnsConflictForDuplicate() throws Exception {
		when(productService.create(any(ProductRequest.class)))
				.thenThrow(new DuplicateResourceException("Product already exists"));

		mockMvc.perform(post("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isConflict());
	}

	@Test
	void updateReturnsOk() throws Exception {
		when(productService.update(any(Long.class), any(ProductRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/products/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isOk());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/products/1"))
				.andExpect(status().isNoContent());

		verify(productService).delete(1L);
	}

	private String validJson() {
		return "{\"sku\":\"SKU-1\",\"name\":\"Keyboard\","
				+ "\"description\":\"Mechanical\",\"salePrice\":99.90,"
				+ "\"active\":true,\"categoryId\":10}";
	}

	private ProductResponse response() {
		return new ProductResponse(
				1L, "SKU-1", "Keyboard", "Mechanical", new BigDecimal("99.90"),
				true, 10L, "Hardware", null, null
		);
	}
}
