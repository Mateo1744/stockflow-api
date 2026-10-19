package com.stockflow.stockexit.controller;

import com.stockflow.common.dto.PageResponse;
import com.stockflow.stockexit.entity.StockExit;
import com.stockflow.security.CurrentUserService;
import com.stockflow.stockexit.dto.StockExitRequest;
import com.stockflow.stockexit.dto.StockExitResponse;
import com.stockflow.stockexit.service.StockExitService;
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
@RequestMapping("/api/stock-exits")
@Tag(name = "Stock exits")
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
	public ResponseEntity<PageResponse<StockExitResponse>> findAll(
			@RequestParam(required = false) StockExit.Status status,
			@RequestParam(required = false) StockExit.Reason reason,
			@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(stockExitService.findAll(status, reason, pageable));
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
	@Operation(summary = "Confirm a draft stock exit", description = "Locks affected inventory rows, validates availability and decreases stock atomically.")
	public ResponseEntity<StockExitResponse> confirm(@PathVariable Long id) {
		Long performedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(stockExitService.confirm(id, performedByUserId));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<StockExitResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(stockExitService.cancel(id));
	}
}
