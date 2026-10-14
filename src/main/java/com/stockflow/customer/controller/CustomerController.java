package com.stockflow.customer.controller;

import java.util.List;

import com.stockflow.customer.dto.CustomerRequest;
import com.stockflow.customer.dto.CustomerResponse;
import com.stockflow.customer.service.CustomerService;
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
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	public ResponseEntity<List<CustomerResponse>> findAll() {
		return ResponseEntity.ok(customerService.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(customerService.findById(id));
	}

	@PostMapping
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody CustomerRequest request
	) {
		return ResponseEntity.ok(customerService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		customerService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
