package com.stockflow.sale.controller;

import java.math.BigDecimal;
import java.util.List;

import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.sale.dto.SaleRequest;
import com.stockflow.sale.dto.SaleResponse;
import com.stockflow.sale.entity.Sale;
import com.stockflow.sale.service.SaleService;
import com.stockflow.security.CurrentUserService;
import com.stockflow.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

	@Mock
	private SaleService saleService;

	@Mock
	private CurrentUserService currentUserService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new SaleController(saleService, currentUserService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturns200() throws Exception {
		when(saleService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/sales"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].saleNumber").value("SALE-1"));
	}

	@Test
	void findByIdReturns200() throws Exception {
		when(saleService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/sales/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void createReturns201AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(saleService.create(any(SaleRequest.class), any(Long.class))).thenReturn(response());

		mockMvc.perform(post("/api/sales")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validJson()))
				.andExpect(status().isCreated());

		verify(saleService).create(any(SaleRequest.class), org.mockito.ArgumentMatchers.eq(7L));
	}

	@Test
	void completeReturns200AndUsesAuthenticatedUserId() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(saleService.complete(1L, 7L)).thenReturn(response());

		mockMvc.perform(post("/api/sales/1/complete"))
				.andExpect(status().isOk());

		verify(saleService).complete(1L, 7L);
	}

	@Test
	void cancelReturns200WithoutResolvingActor() throws Exception {
		when(saleService.cancel(1L)).thenReturn(response());

		mockMvc.perform(post("/api/sales/1/cancel"))
				.andExpect(status().isOk());

		verify(saleService).cancel(1L);
		verify(currentUserService, never()).getCurrentUser();
	}

	@Test
	void invalidCreateRequestReturns400BeforeResolvingUser() throws Exception {
		mockMvc.perform(post("/api/sales")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"items\":[]}"))
				.andExpect(status().isBadRequest());

		verify(currentUserService, never()).getCurrentUser();
	}

	@Test
	void missingSaleReturns404() throws Exception {
		when(saleService.findById(99L)).thenThrow(new ResourceNotFoundException("Missing"));

		mockMvc.perform(get("/api/sales/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidOperationReturns409() throws Exception {
		when(saleService.cancel(1L)).thenThrow(new InvalidOperationException("Invalid state"));

		mockMvc.perform(post("/api/sales/1/cancel"))
				.andExpect(status().isConflict());
	}

	@Test
	void insufficientStockReturns409() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(currentUser());
		when(saleService.complete(1L, 7L))
				.thenThrow(new InsufficientStockException("Insufficient stock"));

		mockMvc.perform(post("/api/sales/1/complete"))
				.andExpect(status().isConflict());
	}

	private User currentUser() {
		User user = new User();
		user.setId(7L);
		user.setActive(true);
		return user;
	}

	private SaleResponse response() {
		return new SaleResponse(
				1L, "SALE-1", Sale.Status.PENDING, null, null, null, null, null,
				7L, "Ana", "Diaz", "ana@example.com", BigDecimal.TEN, List.of(), null, null
		);
	}

	private String validJson() {
		return "{\"customerId\":null,\"items\":[{\"productId\":2,\"quantity\":1}]}";
	}
}
