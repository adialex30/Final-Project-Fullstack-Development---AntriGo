package com.antrigo.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Item keranjang saat checkout. HANYA berisi referensi (productId, variantId) dan quantity —
 * harga TIDAK PERNAH diterima dari client, seluruh nilai dihitung ulang di server (OrderService).
 */
public record CartItemRequest(
        @NotNull Long productId,
        Long variantId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 200) String note
) {}
