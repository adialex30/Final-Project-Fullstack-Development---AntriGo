package com.antrigo.backend.dto.request;

import com.antrigo.backend.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status,
        String note
) {}
