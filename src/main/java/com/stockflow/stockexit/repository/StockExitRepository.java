package com.stockflow.stockexit.repository;

import java.util.Optional;

import com.stockflow.stockexit.entity.StockExit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockExitRepository extends JpaRepository<StockExit, Long>, JpaSpecificationExecutor<StockExit> {

	Optional<StockExit> findByExitNumber(String exitNumber);

	boolean existsByExitNumber(String exitNumber);
}
