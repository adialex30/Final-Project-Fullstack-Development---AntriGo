package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(), item.getProductNameSnapshot(), item.getVariantNameSnapshot(),
                item.getUnitPriceSnapshot(), item.getQuantity(), item.getLineTotal()
        );
    }
}
