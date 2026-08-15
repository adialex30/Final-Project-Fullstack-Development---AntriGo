package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Category;
import com.antrigo.backend.domain.entity.Product;
import com.antrigo.backend.dto.request.ProductRequest;
import com.antrigo.backend.dto.response.PageResponse;
import com.antrigo.backend.dto.response.ProductResponse;
import com.antrigo.backend.exception.DuplicateResourceException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.repository.CategoryRepository;
import com.antrigo.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "products", key = "T(String).format('%s-%s-%d-%d', #search, #categoryId, #page, #size)")
    public PageResponse<ProductResponse> search(String search, Long categoryId, int page, int size) {
        Page<Product> result = productRepository.search(
                (search == null || search.isBlank()) ? null : search.trim(),
                categoryId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        return PageResponse.from(result.map(ProductResponse::from));
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "reports"}, allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan: " + request.categoryId()));

        String slug = CategoryService.slugify(request.name());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Produk dengan nama serupa sudah ada");
        }

        Product product = Product.builder()
                .category(category)
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .active(request.active() == null || request.active())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = {"products", "reports"}, allEntries = true)
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan: " + request.categoryId()));

        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        // NB: stock TIDAK diubah lewat endpoint ini — perubahan stok wajib lewat StockService
        // supaya selalu tercatat sebagai stock_movement (ledger), tidak pernah "diam-diam" berubah.
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = {"products", "reports"}, allEntries = true)
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}
