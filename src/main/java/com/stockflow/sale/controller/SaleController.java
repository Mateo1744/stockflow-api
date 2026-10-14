package com.stockflow.sale.controller;

import java.util.List;

import com.stockflow.sale.dto.SaleRequest;
import com.stockflow.sale.dto.SaleResponse;
import com.stockflow.sale.service.SaleService;
import com.stockflow.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

	private final SaleService saleService;
	private final CurrentUserService currentUserService;

	public SaleController(SaleService saleService, CurrentUserService currentUserService) {
		this.saleService = saleService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<List<SaleResponse>> findAll() {
		return ResponseEntity.ok(saleService.findAll());
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
	public ResponseEntity<SaleResponse> complete(@PathVariable Long id) {
		Long sellerUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(saleService.complete(id, sellerUserId));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<SaleResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.cancel(id));
	}
}
