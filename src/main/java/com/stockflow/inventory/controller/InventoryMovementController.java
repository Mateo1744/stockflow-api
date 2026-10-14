package com.stockflow.inventory.controller;

import java.util.List;

import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.service.InventoryMovementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-movements")
public class InventoryMovementController {

	private final InventoryMovementService inventoryMovementService;

	public InventoryMovementController(InventoryMovementService inventoryMovementService) {
		this.inventoryMovementService = inventoryMovementService;
	}

	@GetMapping
	public ResponseEntity<List<InventoryMovementResponse>> findAll() {
		return ResponseEntity.ok(inventoryMovementService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<InventoryMovementResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(inventoryMovementService.findById(id));
	}
}
