package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Category;
import com.antrigo.backend.dto.request.CategoryRequest;
import com.antrigo.backend.dto.response.CategoryResponse;
import com.antrigo.backend.exception.DuplicateResourceException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> findAll() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        String slug = slugify(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Kategori dengan nama serupa sudah ada");
        }
        Category category = Category.builder()
                .name(request.name())
                .slug(slug)
                .sortOrder(request.sortOrder())
                .active(true)
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan: " + id));
        category.setName(request.name());
        category.setSortOrder(request.sortOrder());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deactivate(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori tidak ditemukan: " + id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        return noAccents.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
