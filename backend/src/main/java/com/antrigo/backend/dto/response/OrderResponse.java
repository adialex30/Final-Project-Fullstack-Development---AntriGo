package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.entity.Payment;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public record OrderResponse(
        String orderNumber,
        LocalDate businessDate,
        Integer queueNumber,
        String tableNumber,
        String customerName,
        String customerPhone,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        int estimatedWaitMinutes,
        String note,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        PaymentStatus paymentStatus,
        String qrPayload,
        Instant paymentExpiresAt
) {
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Jakarta");

    public static OrderResponse from(Order o) {
        return from(o, null);
    }

    public static OrderResponse from(Order o, Payment payment) {
        boolean showQr = payment != null && payment.getStatus() == PaymentStatus.PENDING
                && o.getStatus() == OrderStatus.AWAITING_PAYMENT;
        return new OrderResponse(
                o.getOrderNumber(), o.getBusinessDate(), o.getQueueNumber(), o.getTableNumber(),
                o.getCustomerName(), o.getCustomerPhone(),
                o.getStatus(), o.getSubtotalAmount(), o.getTotalAmount(), o.getEstimatedWaitMinutes(),
                o.getNote(),
                o.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()),
                o.getCreatedAt(),
                payment != null ? payment.getStatus() : null,
                showQr ? payment.getQrPayload() : null,
                showQr && payment.getExpiresAt() != null
                        ? payment.getExpiresAt().atZone(SERVER_ZONE).toInstant()
                        : null
        );
    }
}