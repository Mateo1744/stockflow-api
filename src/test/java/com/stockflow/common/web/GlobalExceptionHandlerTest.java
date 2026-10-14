package com.stockflow.common.web;

import java.util.Set;

import com.stockflow.category.dto.CategoryRequest;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new ExceptionTestController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void resourceNotFoundProducesStandard404() throws Exception {
		mockMvc.perform(get("/test-errors/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Missing resource"))
				.andExpect(jsonPath("$.path").value("/test-errors/not-found"))
				.andExpect(jsonPath("$.fieldErrors").isMap());
	}

	@Test
	void duplicateResourceProduces409() throws Exception {
		mockMvc.perform(get("/test-errors/duplicate"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void invalidOperationProduces409() throws Exception {
		mockMvc.perform(get("/test-errors/invalid-operation"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Invalid state"));
	}

	@Test
	void insufficientStockProduces409() throws Exception {
		mockMvc.perform(get("/test-errors/insufficient-stock"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Insufficient stock"));
	}

	@Test
	void beanValidationProduces400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/test-errors/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\",\"active\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.active").exists());
	}

	@Test
	void constraintViolationProduces400() throws Exception {
		mockMvc.perform(get("/test-errors/constraint"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"));
	}

	@Test
	void malformedJsonProduces400WithoutInternalDetails() throws Exception {
		mockMvc.perform(post("/test-errors/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed JSON request"))
				.andExpect(jsonPath("$.path").value("/test-errors/validation"));
	}

	@Test
	void unexpectedExceptionProducesSafe500() throws Exception {
		mockMvc.perform(get("/test-errors/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"));
	}

	@RestController
	@RequestMapping("/test-errors")
	private static class ExceptionTestController {

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Missing resource");
		}

		@GetMapping("/duplicate")
		void duplicate() {
			throw new DuplicateResourceException("Duplicate resource");
		}

		@GetMapping("/invalid-operation")
		void invalidOperation() {
			throw new InvalidOperationException("Invalid state");
		}

		@GetMapping("/insufficient-stock")
		void insufficientStock() {
			throw new InsufficientStockException("Insufficient stock");
		}

		@GetMapping("/constraint")
		void constraintViolation() {
			throw new ConstraintViolationException(Set.of());
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("Internal implementation detail");
		}

		@PostMapping("/validation")
		void validation(@Valid @RequestBody CategoryRequest request) {
		}
	}
}
