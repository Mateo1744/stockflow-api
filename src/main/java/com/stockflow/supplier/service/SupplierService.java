package com.stockflow.supplier.service;

import java.util.List;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.dto.PageResponse;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.supplier.dto.SupplierRequest;
import com.stockflow.supplier.dto.SupplierResponse;
import com.stockflow.supplier.entity.Supplier;
import com.stockflow.supplier.mapper.SupplierMapper;
import com.stockflow.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final SupplierMapper supplierMapper;

	public SupplierService(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
		this.supplierRepository = supplierRepository;
		this.supplierMapper = supplierMapper;
	}

	@Transactional(readOnly = true)
	public List<SupplierResponse> findAll() {
		return supplierRepository.findAll().stream()
				.map(supplierMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<SupplierResponse> findAll(String taxId, String name, Boolean active,
			Pageable pageable) {
		Specification<Supplier> spec = Specification.allOf(
				contains("taxId", taxId), contains("name", name), equal("active", active));
		return PageResponse.from(supplierRepository.findAll(spec, pageable).map(supplierMapper::toResponse));
	}

	private Specification<Supplier> contains(String field, String value) {
		return value == null || value.isBlank() ? Specification.unrestricted() : (root, query, cb) ->
				cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
	}

	private Specification<Supplier> equal(String field, Object value) {
		return value == null ? Specification.unrestricted() : (root, query, cb) -> cb.equal(root.get(field), value);
	}

	@Transactional(readOnly = true)
	public SupplierResponse findById(Long id) {
		return supplierMapper.toResponse(findEntityById(id));
	}

	@Transactional
	public SupplierResponse create(SupplierRequest request) {
		if (supplierRepository.existsByTaxId(request.taxId())) {
			throw new DuplicateResourceException(
					"Supplier already exists with tax ID: " + request.taxId()
			);
		}

		Supplier supplier = supplierMapper.toEntity(request);
		return supplierMapper.toResponse(supplierRepository.save(supplier));
	}

	@Transactional
	public SupplierResponse update(Long id, SupplierRequest request) {
		Supplier supplier = findEntityById(id);

		if (!supplier.getTaxId().equals(request.taxId())
				&& supplierRepository.existsByTaxId(request.taxId())) {
			throw new DuplicateResourceException(
					"Supplier already exists with tax ID: " + request.taxId()
			);
		}

		supplierMapper.updateEntity(request, supplier);
		return supplierMapper.toResponse(supplierRepository.save(supplier));
	}

	@Transactional
	public void delete(Long id) {
		Supplier supplier = findEntityById(id);
		supplier.setActive(false);
		supplierRepository.save(supplier);
	}

	private Supplier findEntityById(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Supplier not found with id: " + id
				));
	}
}
