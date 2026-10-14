package com.stockflow.customer.service;

import java.util.List;
import java.util.Optional;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.customer.dto.CustomerRequest;
import com.stockflow.customer.dto.CustomerResponse;
import com.stockflow.customer.entity.Customer;
import com.stockflow.customer.mapper.CustomerMapper;
import com.stockflow.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private CustomerMapper customerMapper;

	@InjectMocks
	private CustomerService customerService;

	@Test
	void findAllReturnsMappedCustomers() {
		Customer customer = customer(1L, "100123");
		CustomerResponse response = response(1L, "100123", true);
		when(customerRepository.findAll()).thenReturn(List.of(customer));
		when(customerMapper.toResponse(customer)).thenReturn(response);

		assertEquals(List.of(response), customerService.findAll());
	}

	@Test
	void findByIdReturnsCustomer() {
		Customer customer = customer(1L, "100123");
		CustomerResponse response = response(1L, "100123", true);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerMapper.toResponse(customer)).thenReturn(response);

		assertSame(response, customerService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenCustomerDoesNotExist() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.findById(99L));
		verifyNoInteractions(customerMapper);
	}

	@Test
	void createSavesCustomerWhenDocumentNumberIsAvailable() {
		CustomerRequest request = request("100123");
		Customer customer = customer(null, "100123");
		Customer saved = customer(1L, "100123");
		CustomerResponse response = response(1L, "100123", true);
		when(customerRepository.existsByDocumentNumber("100123")).thenReturn(false);
		when(customerMapper.toEntity(request)).thenReturn(customer);
		when(customerRepository.save(customer)).thenReturn(saved);
		when(customerMapper.toResponse(saved)).thenReturn(response);

		assertSame(response, customerService.create(request));
	}

	@Test
	void createThrowsWhenDocumentNumberAlreadyExists() {
		CustomerRequest request = request("100123");
		when(customerRepository.existsByDocumentNumber("100123")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> customerService.create(request));
		verify(customerRepository, never()).save(any());
		verifyNoInteractions(customerMapper);
	}

	@Test
	void updateSavesCustomerWhenNewDocumentNumberIsAvailable() {
		CustomerRequest request = request("100456");
		Customer customer = customer(1L, "100123");
		CustomerResponse response = response(1L, "100456", true);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.existsByDocumentNumber("100456")).thenReturn(false);
		when(customerRepository.save(customer)).thenReturn(customer);
		when(customerMapper.toResponse(customer)).thenReturn(response);

		assertSame(response, customerService.update(1L, request));
		verify(customerMapper).updateEntity(request, customer);
	}

	@Test
	void updateAllowsKeepingCurrentDocumentNumber() {
		CustomerRequest request = request("100123");
		Customer customer = customer(1L, "100123");
		CustomerResponse response = response(1L, "100123", true);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.save(customer)).thenReturn(customer);
		when(customerMapper.toResponse(customer)).thenReturn(response);

		assertSame(response, customerService.update(1L, request));
		verify(customerRepository, never()).existsByDocumentNumber(any());
		verify(customerMapper).updateEntity(request, customer);
	}

	@Test
	void updateThrowsWhenDocumentNumberBelongsToAnotherCustomer() {
		CustomerRequest request = request("100456");
		Customer customer = customer(1L, "100123");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
		when(customerRepository.existsByDocumentNumber("100456")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> customerService.update(1L, request));
		verify(customerMapper, never()).updateEntity(any(), any());
		verify(customerRepository, never()).save(any());
	}

	@Test
	void deleteDeactivatesExistingCustomer() {
		Customer customer = customer(1L, "100123");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		customerService.delete(1L);

		assertFalse(customer.getActive());
		verify(customerRepository).save(customer);
	}

	@Test
	void deleteThrowsWhenCustomerDoesNotExist() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.delete(99L));
		verify(customerRepository, never()).save(any());
	}

	private CustomerRequest request(String documentNumber) {
		return new CustomerRequest(
				documentNumber, "Jane", "Doe", "jane@example.com", "123", "Address", true
		);
	}

	private Customer customer(Long id, String documentNumber) {
		Customer customer = new Customer();
		customer.setId(id);
		customer.setDocumentNumber(documentNumber);
		customer.setFirstName("Jane");
		customer.setLastName("Doe");
		customer.setActive(true);
		return customer;
	}

	private CustomerResponse response(Long id, String documentNumber, Boolean active) {
		return new CustomerResponse(
				id, documentNumber, "Jane", "Doe", "jane@example.com", "123", "Address",
				active, null, null
		);
	}
}
