package com.antrigo.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartItemRequest(
        @NotNull Long productId,
        Long variantId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 200) String note
) {}
