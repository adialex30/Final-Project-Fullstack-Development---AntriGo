package com.antrigo.backend.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        String imageUrl,
        Boolean active
) {}
