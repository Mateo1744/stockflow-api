package com.stockflow.category.service;

import java.util.List;
import java.util.Optional;

import com.stockflow.category.dto.CategoryRequest;
import com.stockflow.category.dto.CategoryResponse;
import com.stockflow.category.entity.Category;
import com.stockflow.category.mapper.CategoryMapper;
import com.stockflow.category.repository.CategoryRepository;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
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
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private CategoryMapper categoryMapper;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void findAllReturnsMappedCategories() {
		Category category = category(1L, "Hardware");
		CategoryResponse response = response(1L, "Hardware", true);
		when(categoryRepository.findAll()).thenReturn(List.of(category));
		when(categoryMapper.toResponse(category)).thenReturn(response);

		List<CategoryResponse> result = categoryService.findAll();

		assertEquals(List.of(response), result);
	}

	@Test
	void findByIdReturnsCategory() {
		Category category = category(1L, "Hardware");
		CategoryResponse response = response(1L, "Hardware", true);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryMapper.toResponse(category)).thenReturn(response);

		assertSame(response, categoryService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenCategoryDoesNotExist() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(99L));
		verifyNoInteractions(categoryMapper);
	}

	@Test
	void createSavesCategoryWhenNameIsAvailable() {
		CategoryRequest request = request("Hardware");
		Category category = category(null, "Hardware");
		Category saved = category(1L, "Hardware");
		CategoryResponse response = response(1L, "Hardware", true);
		when(categoryRepository.existsByNameIgnoreCase("Hardware")).thenReturn(false);
		when(categoryMapper.toEntity(request)).thenReturn(category);
		when(categoryRepository.save(category)).thenReturn(saved);
		when(categoryMapper.toResponse(saved)).thenReturn(response);

		assertSame(response, categoryService.create(request));
	}

	@Test
	void createThrowsWhenNameAlreadyExists() {
		CategoryRequest request = request("Hardware");
		when(categoryRepository.existsByNameIgnoreCase("Hardware")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> categoryService.create(request));
		verify(categoryRepository, never()).save(any());
		verifyNoInteractions(categoryMapper);
	}

	@Test
	void updateSavesCategoryWhenNewNameIsAvailable() {
		CategoryRequest request = request("Components");
		Category category = category(1L, "Hardware");
		CategoryResponse response = response(1L, "Components", true);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByNameIgnoreCase("Components")).thenReturn(false);
		when(categoryRepository.save(category)).thenReturn(category);
		when(categoryMapper.toResponse(category)).thenReturn(response);

		assertSame(response, categoryService.update(1L, request));
		verify(categoryMapper).updateEntity(request, category);
	}

	@Test
	void updateAllowsKeepingCurrentName() {
		CategoryRequest request = request("hardware");
		Category category = category(1L, "Hardware");
		CategoryResponse response = response(1L, "hardware", true);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.save(category)).thenReturn(category);
		when(categoryMapper.toResponse(category)).thenReturn(response);

		assertSame(response, categoryService.update(1L, request));
		verify(categoryRepository, never()).existsByNameIgnoreCase(any());
		verify(categoryMapper).updateEntity(request, category);
	}

	@Test
	void updateThrowsWhenNewNameBelongsToAnotherCategory() {
		CategoryRequest request = request("Components");
		Category category = category(1L, "Hardware");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByNameIgnoreCase("Components")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> categoryService.update(1L, request));
		verify(categoryMapper, never()).updateEntity(any(), any());
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void deleteDeactivatesExistingCategory() {
		Category category = category(1L, "Hardware");
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		categoryService.delete(1L);

		assertFalse(category.getActive());
		verify(categoryRepository).save(category);
	}

	@Test
	void deleteThrowsWhenCategoryDoesNotExist() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(99L));
		verify(categoryRepository, never()).save(any());
	}

	private CategoryRequest request(String name) {
		return new CategoryRequest(name, "Description", true);
	}

	private Category category(Long id, String name) {
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setActive(true);
		return category;
	}

	private CategoryResponse response(Long id, String name, Boolean active) {
		return new CategoryResponse(id, name, "Description", active, null, null);
	}
}
