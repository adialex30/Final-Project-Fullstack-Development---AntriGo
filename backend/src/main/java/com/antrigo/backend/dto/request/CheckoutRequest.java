package com.antrigo.backend.dto.request;

import com.antrigo.backend.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @Size(max = 20) String tableNumber,
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 300) String note
) {}
