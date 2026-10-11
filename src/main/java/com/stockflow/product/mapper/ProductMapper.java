package com.stockflow.product.mapper;

import com.stockflow.category.entity.Category;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import com.stockflow.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

	public ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getSku(),
				product.getName(),
				product.getDescription(),
				product.getSalePrice(),
				product.getActive(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}

	public Product toEntity(ProductRequest request, Category category) {
		Product product = new Product();
		updateEntity(request, category, product);
		return product;
	}

	public void updateEntity(ProductRequest request, Category category, Product product) {
		product.setSku(request.sku());
		product.setName(request.name());
		product.setDescription(request.description());
		product.setSalePrice(request.salePrice());
		product.setActive(request.active());
		product.setCategory(category);
	}
}
