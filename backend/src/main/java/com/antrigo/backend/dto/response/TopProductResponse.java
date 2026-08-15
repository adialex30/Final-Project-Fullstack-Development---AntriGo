package com.antrigo.backend.dto.response;

import java.math.BigDecimal;

public record TopProductResponse(Long productId, String productName, long totalQty, BigDecimal totalRevenue) {}
