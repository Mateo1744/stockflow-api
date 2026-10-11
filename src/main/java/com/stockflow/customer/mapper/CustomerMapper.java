package com.stockflow.customer.mapper;

import com.stockflow.customer.dto.CustomerRequest;
import com.stockflow.customer.dto.CustomerResponse;
import com.stockflow.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getDocumentNumber(),
				customer.getFirstName(),
				customer.getLastName(),
				customer.getEmail(),
				customer.getPhone(),
				customer.getAddress(),
				customer.getActive(),
				customer.getCreatedAt(),
				customer.getUpdatedAt()
		);
	}

	public Customer toEntity(CustomerRequest request) {
		Customer customer = new Customer();
		updateEntity(request, customer);
		return customer;
	}

	public void updateEntity(CustomerRequest request, Customer customer) {
		customer.setDocumentNumber(request.documentNumber());
		customer.setFirstName(request.firstName());
		customer.setLastName(request.lastName());
		customer.setEmail(request.email());
		customer.setPhone(request.phone());
		customer.setAddress(request.address());
		customer.setActive(request.active());
	}
}
