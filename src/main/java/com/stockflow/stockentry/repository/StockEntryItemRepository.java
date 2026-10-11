package com.stockflow.stockentry.repository;

import com.stockflow.stockentry.entity.StockEntryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryItemRepository extends JpaRepository<StockEntryItem, Long> {
}
