package com.antrigo.backend.dto.response;

public record LowStockResponse(Long productId, String productName, int stock, int threshold) {}
