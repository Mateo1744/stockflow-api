package com.stockflow.stockexit.mapper;

import com.stockflow.stockexit.dto.StockExitItemResponse;
import com.stockflow.stockexit.entity.StockExitItem;
import org.springframework.stereotype.Component;

@Component
public class StockExitItemMapper {

	public StockExitItemResponse toResponse(StockExitItem item) {
		return new StockExitItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getSku(),
				item.getProduct().getName(),
				item.getQuantity()
		);
	}
}
