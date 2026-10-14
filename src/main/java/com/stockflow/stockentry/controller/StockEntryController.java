package com.stockflow.stockentry.controller;

import java.util.List;

import com.stockflow.security.CurrentUserService;
import com.stockflow.stockentry.dto.StockEntryRequest;
import com.stockflow.stockentry.dto.StockEntryResponse;
import com.stockflow.stockentry.service.StockEntryService;
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
@RequestMapping("/api/stock-entries")
public class StockEntryController {

	private final StockEntryService stockEntryService;
	private final CurrentUserService currentUserService;

	public StockEntryController(
			StockEntryService stockEntryService,
			CurrentUserService currentUserService
	) {
		this.stockEntryService = stockEntryService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<List<StockEntryResponse>> findAll() {
		return ResponseEntity.ok(stockEntryService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<StockEntryResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(stockEntryService.findById(id));
	}

	@PostMapping
	public ResponseEntity<StockEntryResponse> create(
			@Valid @RequestBody StockEntryRequest request
	) {
		Long receivedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(stockEntryService.create(request, receivedByUserId));
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<StockEntryResponse> confirm(@PathVariable Long id) {
		Long receivedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(stockEntryService.confirm(id, receivedByUserId));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<StockEntryResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(stockEntryService.cancel(id));
	}
}
