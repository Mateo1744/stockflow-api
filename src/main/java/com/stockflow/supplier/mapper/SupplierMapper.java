package com.stockflow.supplier.mapper;

import com.stockflow.supplier.dto.SupplierRequest;
import com.stockflow.supplier.dto.SupplierResponse;
import com.stockflow.supplier.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

	public SupplierResponse toResponse(Supplier supplier) {
		return new SupplierResponse(
				supplier.getId(),
				supplier.getName(),
				supplier.getTaxId(),
				supplier.getContactName(),
				supplier.getEmail(),
				supplier.getPhone(),
				supplier.getAddress(),
				supplier.getActive(),
				supplier.getCreatedAt(),
				supplier.getUpdatedAt()
		);
	}

	public Supplier toEntity(SupplierRequest request) {
		Supplier supplier = new Supplier();
		updateEntity(request, supplier);
		return supplier;
	}

	public void updateEntity(SupplierRequest request, Supplier supplier) {
		supplier.setName(request.name());
		supplier.setTaxId(request.taxId());
		supplier.setContactName(request.contactName());
		supplier.setEmail(request.email());
		supplier.setPhone(request.phone());
		supplier.setAddress(request.address());
		supplier.setActive(request.active());
	}
}
