package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String slug,
        String description,
        BigDecimal price,
        int stock,
        String imageUrl,
        boolean active,
        List<ProductVariantResponse> variants
) {
    public static ProductResponse from(Product p) {
        List<ProductVariantResponse> variants = p.getVariants() == null ? List.of() :
                p.getVariants().stream()
                        .filter(v -> v.isActive())
                        .map(ProductVariantResponse::from)
                        .collect(Collectors.toList());
        return new ProductResponse(
                p.getId(), p.getCategory().getId(), p.getCategory().getName(),
                p.getName(), p.getSlug(), p.getDescription(), p.getPrice(), p.getStock(),
                p.getImageUrl(), p.isActive(), variants
        );
    }
}
