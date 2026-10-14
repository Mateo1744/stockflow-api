package com.stockflow.inventory.service;

import java.util.List;

import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.dto.InventoryResponse;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.mapper.InventoryMapper;
import com.stockflow.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

	private final InventoryRepository inventoryRepository;
	private final InventoryMapper inventoryMapper;

	public InventoryService(
			InventoryRepository inventoryRepository,
			InventoryMapper inventoryMapper
	) {
		this.inventoryRepository = inventoryRepository;
		this.inventoryMapper = inventoryMapper;
	}

	@Transactional(readOnly = true)
	public List<InventoryResponse> findAll() {
		return inventoryRepository.findAll().stream()
				.map(inventoryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public InventoryResponse findById(Long id) {
		return inventoryMapper.toResponse(findEntityById(id));
	}

	@Transactional(readOnly = true)
	public InventoryResponse findByProductId(Long productId) {
		return inventoryMapper.toResponse(findEntityByProductId(productId));
	}

	private Inventory findEntityById(Long id) {
		return inventoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory not found with id: " + id
				));
	}

	private Inventory findEntityByProductId(Long productId) {
		return inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Inventory not found for product id: " + productId
				));
	}
}
