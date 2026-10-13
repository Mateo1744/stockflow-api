package com.stockflow.sale.mapper;

import com.stockflow.sale.dto.SaleItemResponse;
import com.stockflow.sale.entity.SaleItem;
import org.springframework.stereotype.Component;

@Component
public class SaleItemMapper {

	public SaleItemResponse toResponse(SaleItem item) {
		return new SaleItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getSku(),
				item.getProduct().getName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getSubtotal()
		);
	}
}
