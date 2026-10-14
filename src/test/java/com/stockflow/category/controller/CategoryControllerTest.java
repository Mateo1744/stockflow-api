package com.stockflow.category.controller;

import java.util.List;

import com.stockflow.category.dto.CategoryRequest;
import com.stockflow.category.dto.CategoryResponse;
import com.stockflow.category.service.CategoryService;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
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
class CategoryControllerTest {

	@Mock
	private CategoryService categoryService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new CategoryController(categoryService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(categoryService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Hardware"));
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(categoryService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/categories/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		when(categoryService.findById(99L))
				.thenThrow(new ResourceNotFoundException("Category not found with id: 99"));

		mockMvc.perform(get("/api/categories/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path").value("/api/categories/99"));
	}

	@Test
	void createReturnsCreated() throws Exception {
		when(categoryService.create(any(CategoryRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/categories")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Hardware"));
	}

	@Test
	void createReturnsBadRequestForInvalidBody() throws Exception {
		mockMvc.perform(post("/api/categories")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\",\"active\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.active").exists());
	}

	@Test
	void createReturnsConflictForDuplicate() throws Exception {
		when(categoryService.create(any(CategoryRequest.class)))
				.thenThrow(new DuplicateResourceException("Category already exists"));

		mockMvc.perform(post("/api/categories")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void updateReturnsOk() throws Exception {
		when(categoryService.update(any(Long.class), any(CategoryRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/categories/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isOk());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/categories/1"))
				.andExpect(status().isNoContent());

		verify(categoryService).delete(1L);
	}

	private String validJson() {
		return "{\"name\":\"Hardware\",\"description\":\"Parts\",\"active\":true}";
	}

	private CategoryResponse response() {
		return new CategoryResponse(1L, "Hardware", "Parts", true, null, null);
	}
}
