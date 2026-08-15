package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Satu baris papan dapur — daftar pesanan berurutan menurut nomor antrean. */
public record KitchenBoardResponse(
        String orderNumber,
        int queueNumber,
        String tableNumber,
        OrderStatus status,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        int waitingMinutes
) {}
