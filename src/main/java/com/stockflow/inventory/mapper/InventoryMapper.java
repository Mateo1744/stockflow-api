package com.stockflow.inventory.mapper;

import com.stockflow.inventory.dto.InventoryResponse;
import com.stockflow.inventory.entity.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

	public InventoryResponse toResponse(Inventory inventory) {
		return new InventoryResponse(
				inventory.getId(),
				inventory.getProduct().getId(),
				inventory.getProduct().getSku(),
				inventory.getProduct().getName(),
				inventory.getCurrentStock(),
				inventory.getMinimumStock(),
				inventory.getUpdatedAt()
		);
	}
}
