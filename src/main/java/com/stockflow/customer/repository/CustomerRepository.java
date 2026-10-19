package com.stockflow.customer.repository;

import java.util.Optional;

import com.stockflow.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

	Optional<Customer> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByDocumentNumber(String documentNumber);
}
