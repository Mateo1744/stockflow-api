package com.stockflow.stockexit.controller;

import java.util.List;

import com.stockflow.security.CurrentUserService;
import com.stockflow.stockexit.dto.StockExitRequest;
import com.stockflow.stockexit.dto.StockExitResponse;
import com.stockflow.stockexit.service.StockExitService;
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
@RequestMapping("/api/stock-exits")
public class StockExitController {

	private final StockExitService stockExitService;
	private final CurrentUserService currentUserService;

	public StockExitController(
			StockExitService stockExitService,
			CurrentUserService currentUserService
	) {
		this.stockExitService = stockExitService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<List<StockExitResponse>> findAll() {
		return ResponseEntity.ok(stockExitService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<StockExitResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(stockExitService.findById(id));
	}

	@PostMapping
	public ResponseEntity<StockExitResponse> create(@Valid @RequestBody StockExitRequest request) {
		Long performedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(stockExitService.create(request, performedByUserId));
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<StockExitResponse> confirm(@PathVariable Long id) {
		Long performedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(stockExitService.confirm(id, performedByUserId));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<StockExitResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(stockExitService.cancel(id));
	}
}
