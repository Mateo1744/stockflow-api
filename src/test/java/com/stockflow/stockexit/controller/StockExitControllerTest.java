package com.stockflow.stockexit.controller;

import java.util.List;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.security.CurrentUserService;
import com.stockflow.stockexit.dto.StockExitRequest;
import com.stockflow.stockexit.dto.StockExitResponse;
import com.stockflow.stockexit.entity.StockExit;
import com.stockflow.stockexit.service.StockExitService;
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
class StockExitControllerTest {

	@Mock
	private StockExitService stockExitService;

	@Mock
	private CurrentUserService currentUserService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new StockExitController(stockExitService, currentUserService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturns200() throws Exception {
		when(stockExitService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/stock-exits"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].exitNumber").value("EXIT-1"));
	}

	@Test
	void findByIdReturns200() throws Exception {
		when(stockExitService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/stock-exits/1"))
				.andExpect(status().isOk());
	}

	@Test
	void createReturns201AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(stockExitService.create(any(StockExitRequest.class), eq(7L)))
				.thenReturn(response());

		mockMvc.perform(post("/api/stock-exits")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated());

		verify(stockExitService).create(any(StockExitRequest.class), eq(7L));
	}

	@Test
	void confirmReturns200AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(stockExitService.confirm(1L, 7L)).thenReturn(response());

		mockMvc.perform(post("/api/stock-exits/1/confirm"))
				.andExpect(status().isOk());

		verify(stockExitService).confirm(1L, 7L);
	}

	@Test
	void cancelReturns200WithoutResolvingActor() throws Exception {
		when(stockExitService.cancel(1L)).thenReturn(response());

		mockMvc.perform(post("/api/stock-exits/1/cancel"))
				.andExpect(status().isOk());

		verify(currentUserService, never()).getCurrentUser();
	}

	@Test
	void invalidCreateReturns400() throws Exception {
		mockMvc.perform(post("/api/stock-exits")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"reason\":null,\"items\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingExitReturns404() throws Exception {
		when(stockExitService.findById(99L)).thenThrow(new ResourceNotFoundException("Missing"));

		mockMvc.perform(get("/api/stock-exits/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void insufficientStockReturns409() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(stockExitService.confirm(1L, 7L))
				.thenThrow(new InsufficientStockException("Insufficient"));

		mockMvc.perform(post("/api/stock-exits/1/confirm"))
				.andExpect(status().isConflict());
	}

	private User currentUser() {
		User user = new User();
		user.setId(7L);
		user.setActive(true);
		return user;
	}

	private StockExitResponse response() {
		return new StockExitResponse(
				1L, "EXIT-1", StockExit.Reason.DAMAGED, StockExit.Status.DRAFT, 7L,
				"Ana", "Diaz", "ana@example.com", null, List.of(), null, null, null
		);
	}

	private String validJson() {
		return "{\"reason\":\"DAMAGED\",\"notes\":null,"
				+ "\"items\":[{\"productId\":3,\"quantity\":2}]}";
	}
}
