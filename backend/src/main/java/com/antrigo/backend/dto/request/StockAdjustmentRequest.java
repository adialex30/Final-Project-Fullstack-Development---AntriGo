package com.antrigo.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull Integer quantityChange,
        @Size(max = 300) String note
) {}
