package com.stockflow.stockentry.controller;

import com.stockflow.common.dto.PageResponse;
import com.stockflow.stockentry.entity.StockEntry;
import com.stockflow.security.CurrentUserService;
import com.stockflow.stockentry.dto.StockEntryRequest;
import com.stockflow.stockentry.dto.StockEntryResponse;
import com.stockflow.stockentry.service.StockEntryService;
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
@RequestMapping("/api/stock-entries")
@Tag(name = "Stock entries")
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
	public ResponseEntity<PageResponse<StockEntryResponse>> findAll(
			@RequestParam(required = false) StockEntry.Status status,
			@RequestParam(required = false) Long supplierId,
			@RequestParam(required = false) String referenceNumber,
			@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(stockEntryService.findAll(status, supplierId, referenceNumber, pageable));
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
	@Operation(summary = "Confirm a draft stock entry", description = "Locks affected inventory rows and increases stock atomically.")
	public ResponseEntity<StockEntryResponse> confirm(@PathVariable Long id) {
		Long receivedByUserId = currentUserService.getCurrentUser().getId();
		return ResponseEntity.ok(stockEntryService.confirm(id, receivedByUserId));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<StockEntryResponse> cancel(@PathVariable Long id) {
		return ResponseEntity.ok(stockEntryService.cancel(id));
	}
}
