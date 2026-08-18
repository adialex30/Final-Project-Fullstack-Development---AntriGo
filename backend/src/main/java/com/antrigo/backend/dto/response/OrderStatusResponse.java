package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusResponse(
        String orderNumber,
        Integer queueNumber,
        OrderStatus status,
        int estimatedWaitMinutes,
        LocalDateTime updatedAt
) {
    public static OrderStatusResponse from(Order o) {
        return new OrderStatusResponse(
                o.getOrderNumber(), o.getQueueNumber(), o.getStatus(), o.getEstimatedWaitMinutes(), o.getUpdatedAt()
        );
    }
}
