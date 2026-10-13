package com.stockflow.sale.mapper;

import java.util.List;

import com.stockflow.customer.entity.Customer;
import com.stockflow.sale.dto.SaleItemResponse;
import com.stockflow.sale.dto.SaleResponse;
import com.stockflow.sale.entity.Sale;
import org.springframework.stereotype.Component;

@Component
public class SaleMapper {

	private final SaleItemMapper saleItemMapper;

	public SaleMapper(SaleItemMapper saleItemMapper) {
		this.saleItemMapper = saleItemMapper;
	}

	public SaleResponse toResponse(Sale sale) {
		Customer customer = sale.getCustomer();
		List<SaleItemResponse> items = sale.getItems().stream()
				.map(saleItemMapper::toResponse)
				.toList();

		return new SaleResponse(
				sale.getId(),
				sale.getSaleNumber(),
				sale.getStatus(),
				customer == null ? null : customer.getId(),
				customer == null ? null : customer.getDocumentNumber(),
				customer == null ? null : customer.getFirstName(),
				customer == null ? null : customer.getLastName(),
				customer == null ? null : customer.getEmail(),
				sale.getSeller().getId(),
				sale.getSeller().getFirstName(),
				sale.getSeller().getLastName(),
				sale.getSeller().getEmail(),
				sale.getTotalAmount(),
				items,
				sale.getCreatedAt(),
				sale.getUpdatedAt()
		);
	}
}
