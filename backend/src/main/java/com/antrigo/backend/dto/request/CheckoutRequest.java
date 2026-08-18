package com.antrigo.backend.dto.request;

import com.antrigo.backend.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @Size(max = 20) String tableNumber,
        @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String customerName,
        @NotBlank(message = "Nomor telepon wajib diisi")
        @Pattern(regexp = "^[0-9+][0-9\\-+\\s]{7,19}$", message = "Nomor telepon tidak valid")
        String customerPhone,
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 300) String note
) {}
