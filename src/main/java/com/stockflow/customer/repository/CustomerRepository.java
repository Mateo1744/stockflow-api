package com.stockflow.customer.repository;

import java.util.Optional;

import com.stockflow.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByDocumentNumber(String documentNumber);
}
