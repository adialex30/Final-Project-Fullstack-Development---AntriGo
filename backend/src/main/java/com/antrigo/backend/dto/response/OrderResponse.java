package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record OrderResponse(
        String orderNumber,
        LocalDate businessDate,
        int queueNumber,
        String tableNumber,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        int estimatedWaitMinutes,
        String note,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getOrderNumber(), o.getBusinessDate(), o.getQueueNumber(), o.getTableNumber(),
                o.getStatus(), o.getSubtotalAmount(), o.getTotalAmount(), o.getEstimatedWaitMinutes(),
                o.getNote(),
                o.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()),
                o.getCreatedAt()
        );
    }
}
