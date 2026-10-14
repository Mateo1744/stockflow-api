package com.stockflow.stockentry.mapper;

import java.util.List;

import com.stockflow.stockentry.dto.StockEntryItemResponse;
import com.stockflow.stockentry.dto.StockEntryResponse;
import com.stockflow.stockentry.entity.StockEntry;
import org.springframework.stereotype.Component;

@Component
public class StockEntryMapper {

	private final StockEntryItemMapper stockEntryItemMapper;

	public StockEntryMapper(StockEntryItemMapper stockEntryItemMapper) {
		this.stockEntryItemMapper = stockEntryItemMapper;
	}

	public StockEntryResponse toResponse(StockEntry stockEntry) {
		List<StockEntryItemResponse> items = stockEntry.getItems().stream()
				.map(stockEntryItemMapper::toResponse)
				.toList();

		return new StockEntryResponse(
				stockEntry.getId(),
				stockEntry.getReferenceNumber(),
				stockEntry.getStatus(),
				stockEntry.getSupplier().getId(),
				stockEntry.getSupplier().getName(),
				stockEntry.getReceivedBy().getId(),
				stockEntry.getReceivedBy().getFirstName(),
				stockEntry.getReceivedBy().getLastName(),
				stockEntry.getReceivedBy().getEmail(),
				stockEntry.getTotalCost(),
				stockEntry.getNotes(),
				items,
				stockEntry.getCreatedAt(),
				stockEntry.getUpdatedAt(),
				stockEntry.getConfirmedAt()
		);
	}
}
