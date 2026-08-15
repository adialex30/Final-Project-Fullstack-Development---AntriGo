package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.ProductVariant;

import java.math.BigDecimal;

public record ProductVariantResponse(Long id, String variantGroup, String name, BigDecimal priceDelta) {
    public static ProductVariantResponse from(ProductVariant v) {
        return new ProductVariantResponse(v.getId(), v.getVariantGroup(), v.getName(), v.getPriceDelta());
    }
}
