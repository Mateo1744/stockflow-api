package com.stockflow.category.mapper;

import com.stockflow.category.dto.CategoryRequest;
import com.stockflow.category.dto.CategoryResponse;
import com.stockflow.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	public CategoryResponse toResponse(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				category.getActive(),
				category.getCreatedAt(),
				category.getUpdatedAt()
		);
	}

	public Category toEntity(CategoryRequest request) {
		Category category = new Category();
		updateEntity(request, category);
		return category;
	}

	public void updateEntity(CategoryRequest request, Category category) {
		category.setName(request.name());
		category.setDescription(request.description());
		category.setActive(request.active());
	}
}
