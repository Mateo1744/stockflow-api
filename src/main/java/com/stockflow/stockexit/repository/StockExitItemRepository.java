package com.stockflow.stockexit.repository;

import com.stockflow.stockexit.entity.StockExitItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockExitItemRepository extends JpaRepository<StockExitItem, Long> {
}
