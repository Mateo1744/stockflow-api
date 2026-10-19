package com.stockflow.product.repository;

import java.util.Optional;

import com.stockflow.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	Optional<Product> findBySku(String sku);

	boolean existsBySku(String sku);
}
