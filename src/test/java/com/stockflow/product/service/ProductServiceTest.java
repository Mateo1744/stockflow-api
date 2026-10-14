package com.stockflow.product.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.stockflow.category.entity.Category;
import com.stockflow.category.repository.CategoryRepository;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import com.stockflow.product.entity.Product;
import com.stockflow.product.mapper.ProductMapper;
import com.stockflow.product.repository.ProductRepository;
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
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductMapper productMapper;

	@InjectMocks
	private ProductService productService;

	@Test
	void findAllReturnsMappedProducts() {
		Product product = product(1L, "SKU-1");
		ProductResponse response = response(1L, "SKU-1", true);
		when(productRepository.findAll()).thenReturn(List.of(product));
		when(productMapper.toResponse(product)).thenReturn(response);

		assertEquals(List.of(response), productService.findAll());
	}

	@Test
	void findByIdReturnsProduct() {
		Product product = product(1L, "SKU-1");
		ProductResponse response = response(1L, "SKU-1", true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productMapper.toResponse(product)).thenReturn(response);

		assertSame(response, productService.findById(1L));
	}

	@Test
	void findByIdThrowsWhenProductDoesNotExist() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.findById(99L));
		verifyNoInteractions(productMapper);
	}

	@Test
	void createSavesProductWhenSkuAndCategoryAreValid() {
		ProductRequest request = request("SKU-1", 10L);
		Category category = category(10L);
		Product product = product(null, "SKU-1");
		Product saved = product(1L, "SKU-1");
		ProductResponse response = response(1L, "SKU-1", true);
		when(productRepository.existsBySku("SKU-1")).thenReturn(false);
		when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
		when(productMapper.toEntity(request, category)).thenReturn(product);
		when(productRepository.save(product)).thenReturn(saved);
		when(productMapper.toResponse(saved)).thenReturn(response);

		assertSame(response, productService.create(request));
	}

	@Test
	void createThrowsWhenSkuAlreadyExists() {
		ProductRequest request = request("SKU-1", 10L);
		when(productRepository.existsBySku("SKU-1")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> productService.create(request));
		verifyNoInteractions(categoryRepository, productMapper);
		verify(productRepository, never()).save(any());
	}

	@Test
	void createThrowsWhenCategoryDoesNotExist() {
		ProductRequest request = request("SKU-1", 99L);
		when(productRepository.existsBySku("SKU-1")).thenReturn(false);
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.create(request));
		verifyNoInteractions(productMapper);
		verify(productRepository, never()).save(any());
	}

	@Test
	void updateSavesProductWhenSkuAndCategoryAreValid() {
		ProductRequest request = request("SKU-2", 20L);
		Product product = product(1L, "SKU-1");
		Category category = category(20L);
		ProductResponse response = response(1L, "SKU-2", true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productRepository.existsBySku("SKU-2")).thenReturn(false);
		when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
		when(productRepository.save(product)).thenReturn(product);
		when(productMapper.toResponse(product)).thenReturn(response);

		assertSame(response, productService.update(1L, request));
		verify(productMapper).updateEntity(request, category, product);
	}

	@Test
	void updateAllowsKeepingCurrentSku() {
		ProductRequest request = request("SKU-1", 10L);
		Product product = product(1L, "SKU-1");
		Category category = category(10L);
		ProductResponse response = response(1L, "SKU-1", true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
		when(productRepository.save(product)).thenReturn(product);
		when(productMapper.toResponse(product)).thenReturn(response);

		assertSame(response, productService.update(1L, request));
		verify(productRepository, never()).existsBySku(any());
		verify(productMapper).updateEntity(request, category, product);
	}

	@Test
	void updateThrowsWhenSkuBelongsToAnotherProduct() {
		ProductRequest request = request("SKU-2", 20L);
		Product product = product(1L, "SKU-1");
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productRepository.existsBySku("SKU-2")).thenReturn(true);

		assertThrows(DuplicateResourceException.class, () -> productService.update(1L, request));
		verifyNoInteractions(categoryRepository, productMapper);
		verify(productRepository, never()).save(any());
	}

	@Test
	void updateThrowsWhenCategoryDoesNotExist() {
		ProductRequest request = request("SKU-1", 99L);
		Product product = product(1L, "SKU-1");
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.update(1L, request));
		verify(productRepository, never()).existsBySku(any());
		verifyNoInteractions(productMapper);
		verify(productRepository, never()).save(any());
	}

	@Test
	void deleteDeactivatesExistingProduct() {
		Product product = product(1L, "SKU-1");
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.delete(1L);

		assertFalse(product.getActive());
		verify(productRepository).save(product);
	}

	@Test
	void deleteThrowsWhenProductDoesNotExist() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.delete(99L));
		verify(productRepository, never()).save(any());
	}

	private ProductRequest request(String sku, Long categoryId) {
		return new ProductRequest(sku, "Product", "Description", BigDecimal.TEN, true, categoryId);
	}

	private Product product(Long id, String sku) {
		Product product = new Product();
		product.setId(id);
		product.setSku(sku);
		product.setName("Product");
		product.setSalePrice(BigDecimal.TEN);
		product.setActive(true);
		return product;
	}

	private Category category(Long id) {
		Category category = new Category();
		category.setId(id);
		category.setName("Category");
		category.setActive(true);
		return category;
	}

	private ProductResponse response(Long id, String sku, Boolean active) {
		return new ProductResponse(
				id, sku, "Product", "Description", BigDecimal.TEN, active,
				10L, "Category", null, null
		);
	}
}
