package com.stockflow.inventory.controller;

import com.stockflow.common.dto.PageResponse;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.service.InventoryMovementService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-movements")
public class InventoryMovementController {

	private final InventoryMovementService inventoryMovementService;

	public InventoryMovementController(InventoryMovementService inventoryMovementService) {
		this.inventoryMovementService = inventoryMovementService;
	}

	@GetMapping
	public ResponseEntity<PageResponse<InventoryMovementResponse>> findAll(
			@RequestParam(required = false) Long productId,
			@RequestParam(required = false) InventoryMovement.Type type,
			@RequestParam(required = false) Long performedById,
			@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(inventoryMovementService.findAll(productId, type, performedById, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<InventoryMovementResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(inventoryMovementService.findById(id));
	}
}
