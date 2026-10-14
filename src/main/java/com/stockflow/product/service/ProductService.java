package com.stockflow.product.service;

import java.util.List;

import com.stockflow.category.entity.Category;
import com.stockflow.category.repository.CategoryRepository;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import com.stockflow.product.entity.Product;
import com.stockflow.product.mapper.ProductMapper;
import com.stockflow.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductMapper productMapper;

	public ProductService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			ProductMapper productMapper
	) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.productMapper = productMapper;
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> findAll() {
		return productRepository.findAll().stream()
				.map(productMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductResponse findById(Long id) {
		return productMapper.toResponse(findEntityById(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		if (productRepository.existsBySku(request.sku())) {
			throw new DuplicateResourceException(
					"Product already exists with SKU: " + request.sku()
			);
		}

		Category category = findCategoryById(request.categoryId());
		Product product = productMapper.toEntity(request, category);
		return productMapper.toResponse(productRepository.save(product));
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = findEntityById(id);

		if (!product.getSku().equals(request.sku())
				&& productRepository.existsBySku(request.sku())) {
			throw new DuplicateResourceException(
					"Product already exists with SKU: " + request.sku()
			);
		}

		Category category = findCategoryById(request.categoryId());
		productMapper.updateEntity(request, category, product);
		return productMapper.toResponse(productRepository.save(product));
	}

	@Transactional
	public void delete(Long id) {
		Product product = findEntityById(id);
		product.setActive(false);
		productRepository.save(product);
	}

	private Product findEntityById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Product not found with id: " + id
				));
	}

	private Category findCategoryById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Category not found with id: " + id
				));
	}
}
