package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.ProductRequest;
import com.antrigo.backend.dto.response.PageResponse;
import com.antrigo.backend.dto.response.ProductResponse;
import com.antrigo.backend.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Products", description = "Katalog produk — publik untuk baca, admin untuk CRUD & stok")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/v1/products")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.search(search, category, page, size);
    }

    @GetMapping("/api/v1/products/{id}")
    public ProductResponse detail(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping("/api/v1/admin/products")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/api/v1/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }
}
