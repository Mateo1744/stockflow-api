package com.stockflow.inventory.service;

import java.util.List;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.dto.PageResponse;
import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.entity.InventoryMovement;
import com.stockflow.inventory.mapper.InventoryMovementMapper;
import com.stockflow.inventory.repository.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryMovementService {

	private final InventoryMovementRepository inventoryMovementRepository;
	private final InventoryMovementMapper inventoryMovementMapper;

	public InventoryMovementService(
			InventoryMovementRepository inventoryMovementRepository,
			InventoryMovementMapper inventoryMovementMapper
	) {
		this.inventoryMovementRepository = inventoryMovementRepository;
		this.inventoryMovementMapper = inventoryMovementMapper;
	}

	@Transactional(readOnly = true)
	public List<InventoryMovementResponse> findAll() {
		return inventoryMovementRepository.findAll().stream()
				.map(inventoryMovementMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<InventoryMovementResponse> findAll(Long productId,
			InventoryMovement.Type type, Long performedById, Pageable pageable) {
		Specification<InventoryMovement> spec = Specification.allOf(
				productId == null ? Specification.unrestricted() : (root, query, cb) ->
						cb.equal(root.get("inventory").get("product").get("id"), productId),
				type == null ? Specification.unrestricted() : (root, query, cb) -> cb.equal(root.get("type"), type),
				performedById == null ? Specification.unrestricted() : (root, query, cb) ->
						cb.equal(root.get("performedBy").get("id"), performedById));
		return PageResponse.from(inventoryMovementRepository.findAll(spec, pageable)
				.map(inventoryMovementMapper::toResponse));
	}

	@Transactional(readOnly = true)
	public InventoryMovementResponse findById(Long id) {
		return inventoryMovementMapper.toResponse(findEntityById(id));
	}

	private InventoryMovement findEntityById(Long id) {
		return inventoryMovementRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory movement not found with id: " + id
				));
	}
}
