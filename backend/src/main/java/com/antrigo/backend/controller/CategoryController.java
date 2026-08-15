package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.CategoryRequest;
import com.antrigo.backend.dto.response.CategoryResponse;
import com.antrigo.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Kategori menu — publik untuk baca, admin untuk kelola")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/v1/categories")
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        categoryService.deactivate(id);
    }
}
