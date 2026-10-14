package com.stockflow.stockentry.mapper;

import com.stockflow.stockentry.dto.StockEntryItemResponse;
import com.stockflow.stockentry.entity.StockEntryItem;
import org.springframework.stereotype.Component;

@Component
public class StockEntryItemMapper {

	public StockEntryItemResponse toResponse(StockEntryItem item) {
		return new StockEntryItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getSku(),
				item.getProduct().getName(),
				item.getQuantity(),
				item.getUnitCost(),
				item.getSubtotal()
		);
	}
}
