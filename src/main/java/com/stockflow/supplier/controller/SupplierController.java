package com.stockflow.supplier.controller;

import java.util.List;

import com.stockflow.supplier.dto.SupplierRequest;
import com.stockflow.supplier.dto.SupplierResponse;
import com.stockflow.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	public ResponseEntity<List<SupplierResponse>> findAll() {
		return ResponseEntity.ok(supplierService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<SupplierResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(supplierService.findById(id));
	}

	@PostMapping
	public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<SupplierResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody SupplierRequest request
	) {
		return ResponseEntity.ok(supplierService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		supplierService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
