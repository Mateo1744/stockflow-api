package com.stockflow.sale.controller;

import java.time.Instant;

import com.stockflow.common.dto.PageResponse;
import com.stockflow.sale.entity.Sale;
import com.stockflow.sale.dto.SaleRequest;
import com.stockflow.sale.dto.SaleResponse;
import com.stockflow.sale.service.SaleService;
import com.stockflow.security.CurrentUserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales")
public class SaleController {

	private final SaleService saleService;
	private final CurrentUserService currentUserService;

	public SaleController(SaleService saleService, CurrentUserService currentUserService) {
		this.saleService = saleService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<PageResponse<SaleResponse>> findAll(
			@RequestParam(required = false) String saleNumber,
			@RequestParam(required = false) Sale.Status status,
			@RequestParam(required = false) Long customerId,
			@RequestParam(required = false) Long sellerId,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to,
			@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(saleService.findAll(saleNumber, status, customerId, sellerId, from, to, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<SaleResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.findById(id));
	}

	@PostMapping
	public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
		Long sellerUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(saleService.create(request, sellerUserId));
	}

	@PostMapping("/{id}/complete")
	@Operation(summary = "Complete a pending sale", description = "Locks affected inventory rows in product-id order, validates stock and records movements atomically.")
	public ResponseEntity<SaleResponse> complete(@PathVariable Long id) {
		Long sellerUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(saleService.complete(id, sellerUserId));
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel a pending sale")
	public ResponseEntity<SaleResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.cancel(id));
	}
}
