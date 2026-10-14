package com.stockflow.category.service;

import java.util.List;

import com.stockflow.category.dto.CategoryRequest;
import com.stockflow.category.dto.CategoryResponse;
import com.stockflow.category.entity.Category;
import com.stockflow.category.mapper.CategoryMapper;
import com.stockflow.category.repository.CategoryRepository;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
		this.categoryRepository = categoryRepository;
		this.categoryMapper = categoryMapper;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAll().stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return categoryMapper.toResponse(findEntityById(id));
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsByNameIgnoreCase(request.name())) {
			throw new DuplicateResourceException(
					"Category already exists with name: " + request.name()
			);
		}

		Category category = categoryMapper.toEntity(request);
		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = findEntityById(id);

		if (!category.getName().equalsIgnoreCase(request.name())
				&& categoryRepository.existsByNameIgnoreCase(request.name())) {
			throw new DuplicateResourceException(
					"Category already exists with name: " + request.name()
			);
		}

		categoryMapper.updateEntity(request, category);
		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Transactional
	public void delete(Long id) {
		Category category = findEntityById(id);
		category.setActive(false);
		categoryRepository.save(category);
	}

	private Category findEntityById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Category not found with id: " + id
				));
	}
}
