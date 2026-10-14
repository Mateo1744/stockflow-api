package com.stockflow.stockentry.controller;

import java.math.BigDecimal;
import java.util.List;

import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.security.CurrentUserService;
import com.stockflow.stockentry.dto.StockEntryRequest;
import com.stockflow.stockentry.dto.StockEntryResponse;
import com.stockflow.stockentry.entity.StockEntry;
import com.stockflow.stockentry.service.StockEntryService;
import com.stockflow.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class StockEntryControllerTest {

	@Mock
	private StockEntryService stockEntryService;

	@Mock
	private CurrentUserService currentUserService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(
				new StockEntryController(stockEntryService, currentUserService)
		).setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void findAllReturns200() throws Exception {
		when(stockEntryService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/stock-entries"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].referenceNumber").value("REF-1"));
	}

	@Test
	void findByIdReturns200() throws Exception {
		when(stockEntryService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/stock-entries/1"))
				.andExpect(status().isOk());
	}

	@Test
	void createReturns201AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(stockEntryService.create(any(StockEntryRequest.class), eq(7L)))
				.thenReturn(response());

		mockMvc.perform(post("/api/stock-entries")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated());

		verify(stockEntryService).create(any(StockEntryRequest.class), eq(7L));
	}

	@Test
	void confirmReturns200AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(stockEntryService.confirm(1L, 7L)).thenReturn(response());

		mockMvc.perform(post("/api/stock-entries/1/confirm"))
				.andExpect(status().isOk());

		verify(stockEntryService).confirm(1L, 7L);
	}

	@Test
	void cancelReturns200WithoutResolvingActor() throws Exception {
		when(stockEntryService.cancel(1L)).thenReturn(response());

		mockMvc.perform(post("/api/stock-entries/1/cancel"))
				.andExpect(status().isOk());

		verify(currentUserService, never()).getCurrentUser();
	}

	@Test
	void invalidCreateReturns400() throws Exception {
		mockMvc.perform(post("/api/stock-entries")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"supplierId\":null,\"items\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingEntryReturns404() throws Exception {
		when(stockEntryService.findById(99L)).thenThrow(new ResourceNotFoundException("Missing"));

		mockMvc.perform(get("/api/stock-entries/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidOperationReturns409() throws Exception {
		when(stockEntryService.cancel(1L)).thenThrow(new InvalidOperationException("Invalid"));

		mockMvc.perform(post("/api/stock-entries/1/cancel"))
				.andExpect(status().isConflict());
	}

	private User currentUser() {
		User user = new User();
		user.setId(7L);
		user.setActive(true);
		return user;
	}

	private StockEntryResponse response() {
		return new StockEntryResponse(
				1L, "REF-1", StockEntry.Status.DRAFT, 2L, "Supplier", 7L,
				"Ana", "Diaz", "ana@example.com", BigDecimal.TEN, null,
				List.of(), null, null, null
		);
	}

	private String validJson() {
		return "{\"supplierId\":2,\"referenceNumber\":\"REF-1\","
				+ "\"notes\":null,\"items\":[{\"productId\":3,"
				+ "\"quantity\":2,\"unitCost\":5.00}]}";
	}
}
