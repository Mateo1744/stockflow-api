package com.stockflow.supplier.service;

import java.util.List;
import java.util.Optional;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.supplier.dto.SupplierRequest;
import com.stockflow.supplier.dto.SupplierResponse;
import com.stockflow.supplier.entity.Supplier;
import com.stockflow.supplier.mapper.SupplierMapper;
import com.stockflow.supplier.repository.SupplierRepository;
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
class SupplierServiceTest {

	@Mock
	private SupplierRepository supplierRepository;

	@Mock
	private SupplierMapper supplierMapper;

	@InjectMocks
	private SupplierService supplierService;

	@Test
	void findAllReturnsMappedSuppliers() {
		Supplier supplier = supplier(1L, "900123");
		SupplierResponse response = response(1L, "900123", true);
		when(supplierRepository.findAll()).thenReturn(List.of(supplier));
		when(supplierMapper.toResponse(supplier)).thenReturn(response);

		assertEquals(List.of(response), supplierService.findAll());
	}

	@Test
	void findByIdReturnsSupplier() {
		Supplier supplier = supplier(1L, "900123");
		SupplierResponse response = response(1L, "900123", true);
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
		when(supplierMapper.toResponse(supplier)).thenReturn(response);

		assertSame(response, supplierService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenSupplierDoesNotExist() {
		when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.findById(99L));
		verifyNoInteractions(supplierMapper);
	}

	@Test
	void createSavesSupplierWhenTaxIdIsAvailable() {
		SupplierRequest request = request("900123");
		Supplier supplier = supplier(null, "900123");
		Supplier saved = supplier(1L, "900123");
		SupplierResponse response = response(1L, "900123", true);
		when(supplierRepository.existsByTaxId("900123")).thenReturn(false);
		when(supplierMapper.toEntity(request)).thenReturn(supplier);
		when(supplierRepository.save(supplier)).thenReturn(saved);
		when(supplierMapper.toResponse(saved)).thenReturn(response);

		assertSame(response, supplierService.create(request));
	}

	@Test
	void createThrowsWhenTaxIdAlreadyExists() {
		SupplierRequest request = request("900123");
		when(supplierRepository.existsByTaxId("900123")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> supplierService.create(request));
		verify(supplierRepository, never()).save(any());
		verifyNoInteractions(supplierMapper);
	}

	@Test
	void updateSavesSupplierWhenNewTaxIdIsAvailable() {
		SupplierRequest request = request("900456");
		Supplier supplier = supplier(1L, "900123");
		SupplierResponse response = response(1L, "900456", true);
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
		when(supplierRepository.existsByTaxId("900456")).thenReturn(false);
		when(supplierRepository.save(supplier)).thenReturn(supplier);
		when(supplierMapper.toResponse(supplier)).thenReturn(response);

		assertSame(response, supplierService.update(1L, request));
		verify(supplierMapper).updateEntity(request, supplier);
	}

	@Test
	void updateAllowsKeepingCurrentTaxId() {
		SupplierRequest request = request("900123");
		Supplier supplier = supplier(1L, "900123");
		SupplierResponse response = response(1L, "900123", true);
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
		when(supplierRepository.save(supplier)).thenReturn(supplier);
		when(supplierMapper.toResponse(supplier)).thenReturn(response);

		assertSame(response, supplierService.update(1L, request));
		verify(supplierRepository, never()).existsByTaxId(any());
		verify(supplierMapper).updateEntity(request, supplier);
	}

	@Test
	void updateThrowsWhenNewTaxIdBelongsToAnotherSupplier() {
		SupplierRequest request = request("900456");
		Supplier supplier = supplier(1L, "900123");
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
		when(supplierRepository.existsByTaxId("900456")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> supplierService.update(1L, request));
		verify(supplierMapper, never()).updateEntity(any(), any());
		verify(supplierRepository, never()).save(any());
	}

	@Test
	void deleteDeactivatesExistingSupplier() {
		Supplier supplier = supplier(1L, "900123");
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		supplierService.delete(1L);

		assertFalse(supplier.getActive());
		verify(supplierRepository).save(supplier);
	}

	@Test
	void deleteThrowsWhenSupplierDoesNotExist() {
		when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.delete(99L));
		verify(supplierRepository, never()).save(any());
	}

	private SupplierRequest request(String taxId) {
		return new SupplierRequest(
				"Supplier", taxId, "Contact", "supplier@example.com", "123", "Address", true
		);
	}

	private Supplier supplier(Long id, String taxId) {
		Supplier supplier = new Supplier();
		supplier.setId(id);
		supplier.setName("Supplier");
		supplier.setTaxId(taxId);
		supplier.setActive(true);
		return supplier;
	}

	private SupplierResponse response(Long id, String taxId, Boolean active) {
		return new SupplierResponse(
				id, "Supplier", taxId, "Contact", "supplier@example.com", "123", "Address",
				active, null, null
		);
	}
}
