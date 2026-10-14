package com.stockflow.inventory.controller;

import java.util.List;

import com.stockflow.inventory.dto.InventoryResponse;
import com.stockflow.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping
	public ResponseEntity<List<InventoryResponse>> findAll() {
		return ResponseEntity.ok(inventoryService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<InventoryResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(inventoryService.findById(id));
	}

	@GetMapping("/product/{productId}")
	public ResponseEntity<InventoryResponse> findByProductId(@PathVariable Long productId) {
		return ResponseEntity.ok(inventoryService.findByProductId(productId));
	}
}
