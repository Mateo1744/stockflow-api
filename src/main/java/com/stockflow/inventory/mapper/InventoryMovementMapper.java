package com.stockflow.inventory.mapper;

import com.stockflow.inventory.dto.InventoryMovementResponse;
import com.stockflow.inventory.entity.InventoryMovement;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementMapper {

	public InventoryMovementResponse toResponse(InventoryMovement movement) {
		return new InventoryMovementResponse(
				movement.getId(),
				movement.getInventory().getId(),
				movement.getInventory().getProduct().getId(),
				movement.getInventory().getProduct().getSku(),
				movement.getInventory().getProduct().getName(),
				movement.getType(),
				movement.getQuantity(),
				movement.getPreviousStock(),
				movement.getResultingStock(),
				movement.getPerformedBy().getId(),
				movement.getPerformedBy().getFirstName(),
				movement.getPerformedBy().getLastName(),
				movement.getPerformedBy().getEmail(),
				movement.getNotes(),
				movement.getCreatedAt()
		);
	}
}
