package com.stockflow.customer.service;

import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.dto.PageResponse;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.customer.dto.CustomerRequest;
import com.stockflow.customer.dto.CustomerResponse;
import com.stockflow.customer.entity.Customer;
import com.stockflow.customer.mapper.CustomerMapper;
import com.stockflow.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final CustomerMapper customerMapper;

	public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
		this.customerRepository = customerRepository;
		this.customerMapper = customerMapper;
	}

	@Transactional(readOnly = true)
	public List<CustomerResponse> findAll() {
		return customerRepository.findAll().stream()
				.map(customerMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<CustomerResponse> findAll(String documentNumber, String name,
			Boolean active, Pageable pageable) {
		Specification<Customer> spec = Specification.allOf(
				contains("documentNumber", documentNumber), contains("name", name), equal("active", active));
		return PageResponse.from(customerRepository.findAll(spec, pageable).map(customerMapper::toResponse));
	}

	private Specification<Customer> contains(String field, String value) {
		return value == null || value.isBlank() ? null : (root, query, cb) ->
				cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
	}

	private Specification<Customer> equal(String field, Object value) {
		return value == null ? null : (root, query, cb) -> cb.equal(root.get(field), value);
	}

	@Transactional(readOnly = true)
	public CustomerResponse findById(Long id) {
		return customerMapper.toResponse(findEntityById(id));
	}

	@Transactional
	public CustomerResponse create(CustomerRequest request) {
		if (customerRepository.existsByDocumentNumber(request.documentNumber())) {
			throw new DuplicateResourceException(
					"Customer already exists with document number: " + request.documentNumber()
			);
		}

		Customer customer = customerMapper.toEntity(request);
		return customerMapper.toResponse(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = findEntityById(id);

		if (!customer.getDocumentNumber().equals(request.documentNumber())
				&& customerRepository.existsByDocumentNumber(request.documentNumber())) {
			throw new DuplicateResourceException(
					"Customer already exists with document number: " + request.documentNumber()
			);
		}

		customerMapper.updateEntity(request, customer);
		return customerMapper.toResponse(customerRepository.save(customer));
	}

	@Transactional
	public void delete(Long id) {
		Customer customer = findEntityById(id);
		customer.setActive(false);
		customerRepository.save(customer);
	}

	private Customer findEntityById(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Customer not found with id: " + id
				));
	}
}
