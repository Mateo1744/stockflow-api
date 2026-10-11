package com.stockflow.stockexit.repository;

import java.util.Optional;

import com.stockflow.stockexit.entity.StockExit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockExitRepository extends JpaRepository<StockExit, Long> {

	Optional<StockExit> findByExitNumber(String exitNumber);

	boolean existsByExitNumber(String exitNumber);
}
