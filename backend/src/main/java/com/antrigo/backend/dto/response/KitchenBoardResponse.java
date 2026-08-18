package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentMethod;
import com.antrigo.backend.domain.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record KitchenBoardResponse(
        String orderNumber,
        Integer queueNumber,
        String tableNumber,
        String customerName,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        int waitingMinutes
) {}