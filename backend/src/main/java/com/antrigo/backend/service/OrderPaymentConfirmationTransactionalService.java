package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.entity.OrderStatusLog;
import com.antrigo.backend.domain.entity.Payment;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentStatus;
import com.antrigo.backend.dto.response.OrderResponse;
import com.antrigo.backend.exception.InvalidStatusTransitionException;
import com.antrigo.backend.exception.PaymentSessionExpiredException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.repository.OrderRepository;
import com.antrigo.backend.repository.OrderStatusLogRepository;
import com.antrigo.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderPaymentConfirmationTransactionalService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final QueueService queueService;
    private final PaymentExpiryHelper expiryHelper;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public OrderResponse execute(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan: " + orderNumber));
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment untuk pesanan ini tidak ditemukan"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return OrderResponse.from(order, payment);
        }

        if (expiryHelper.expireIfDue(order, payment)) {
            throw new PaymentSessionExpiredException(
                    "Sesi pembayaran sudah kedaluwarsa, silakan buat pesanan baru");
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentSessionExpiredException(
                    "Sesi pembayaran untuk pesanan ini sudah tidak berlaku");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        LocalDate businessDate = order.getBusinessDate();
        int queueNumber = queueService.nextQueueNumber(businessDate);
        order.setQueueNumber(queueNumber);
        order.setEstimatedWaitMinutes(queueService.estimateWaitMinutes(businessDate, queueNumber));

        if (!order.getStatus().canTransitionTo(OrderStatus.QUEUED)) {
            throw new InvalidStatusTransitionException(
                    "Pesanan tidak bisa diproses ke antrean dari status " + order.getStatus());
        }
        String from = order.getStatus().name();
        order.setStatus(OrderStatus.QUEUED);
        orderRepository.save(order);

        statusLogRepository.save(OrderStatusLog.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(OrderStatus.QUEUED.name())
                .note("Pembayaran QRIS dikonfirmasi (dummy Midtrans) — masuk antrean")
                .build());

        return OrderResponse.from(order, payment);
    }
}
