package com.stockflow.product.service;

import java.util.List;

import com.stockflow.category.entity.Category;
import com.stockflow.common.dto.PageResponse;
import com.stockflow.category.repository.CategoryRepository;
import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.inventory.entity.Inventory;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.product.dto.ProductRequest;
import com.stockflow.product.dto.ProductResponse;
import com.stockflow.product.entity.Product;
import com.stockflow.product.mapper.ProductMapper;
import com.stockflow.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final InventoryRepository inventoryRepository;
	private final ProductMapper productMapper;

	public ProductService(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			InventoryRepository inventoryRepository,
			ProductMapper productMapper
	) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.inventoryRepository = inventoryRepository;
		this.productMapper = productMapper;
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> findAll() {
		return productRepository.findAll().stream()
				.map(productMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> findAll(String sku, String name, Boolean active,
			Long categoryId, Pageable pageable) {
		Specification<Product> spec = Specification.allOf(
				containsIgnoreCase("sku", sku), containsIgnoreCase("name", name),
				equalsValue("active", active), equalsNested("category", "id", categoryId));
		return PageResponse.from(productRepository.findAll(spec, pageable).map(productMapper::toResponse));
	}

	private Specification<Product> containsIgnoreCase(String field, String value) {
		return value == null || value.isBlank() ? null : (root, query, cb) ->
				cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
	}

	private Specification<Product> equalsValue(String field, Object value) {
		return value == null ? null : (root, query, cb) -> cb.equal(root.get(field), value);
	}

	private Specification<Product> equalsNested(String association, String field, Object value) {
		return value == null ? null : (root, query, cb) -> cb.equal(root.get(association).get(field), value);
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
		Product savedProduct = productRepository.save(product);

		Inventory inventory = new Inventory();
		inventory.setProduct(savedProduct);
		inventory.setCurrentStock(0);
		inventory.setMinimumStock(0);
		inventoryRepository.save(inventory);

		return productMapper.toResponse(savedProduct);
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
