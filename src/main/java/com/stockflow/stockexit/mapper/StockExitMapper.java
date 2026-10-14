package com.stockflow.stockexit.mapper;

import java.util.List;

import com.stockflow.stockexit.dto.StockExitItemResponse;
import com.stockflow.stockexit.dto.StockExitResponse;
import com.stockflow.stockexit.entity.StockExit;
import org.springframework.stereotype.Component;

@Component
public class StockExitMapper {

	private final StockExitItemMapper stockExitItemMapper;

	public StockExitMapper(StockExitItemMapper stockExitItemMapper) {
		this.stockExitItemMapper = stockExitItemMapper;
	}

	public StockExitResponse toResponse(StockExit stockExit) {
		List<StockExitItemResponse> items = stockExit.getItems().stream()
				.map(stockExitItemMapper::toResponse)
				.toList();

		return new StockExitResponse(
				stockExit.getId(),
				stockExit.getExitNumber(),
				stockExit.getReason(),
				stockExit.getStatus(),
				stockExit.getPerformedBy().getId(),
				stockExit.getPerformedBy().getFirstName(),
				stockExit.getPerformedBy().getLastName(),
				stockExit.getPerformedBy().getEmail(),
				stockExit.getNotes(),
				items,
				stockExit.getCreatedAt(),
				stockExit.getUpdatedAt(),
				stockExit.getConfirmedAt()
		);
	}
}
