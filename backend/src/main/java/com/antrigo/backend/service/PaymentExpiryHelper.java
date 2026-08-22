package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.entity.OrderItem;
import com.antrigo.backend.domain.entity.OrderStatusLog;
import com.antrigo.backend.domain.entity.Payment;
import com.antrigo.backend.domain.entity.Product;
import com.antrigo.backend.domain.entity.StockMovement;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentStatus;
import com.antrigo.backend.domain.enums.StockMovementType;
import com.antrigo.backend.repository.OrderRepository;
import com.antrigo.backend.repository.OrderStatusLogRepository;
import com.antrigo.backend.repository.PaymentRepository;
import com.antrigo.backend.repository.ProductRepository;
import com.antrigo.backend.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentExpiryHelper {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public boolean expireIfDue(Order order, Payment payment) {
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        if (payment.getExpiresAt() == null || payment.getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        statusLogRepository.save(OrderStatusLog.builder()
                .order(order)
                .fromStatus(OrderStatus.AWAITING_PAYMENT.name())
                .toStatus(OrderStatus.CANCELLED.name())
                .note("Sesi pembayaran QRIS kedaluwarsa — direset otomatis")
                .build());

        for (OrderItem item : order.getItems()) {
            productRepository.findByIdForUpdate(item.getProduct().getId()).ifPresent(product -> {
                reverseStockForItem(order, product, item.getQuantity());
            });
        }
        return true;
    }

    private void reverseStockForItem(Order order, Product product, int quantity) {
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);

        stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .type(StockMovementType.CANCELLATION_REVERSAL)
                .quantityChange(quantity)
                .referenceType("ORDER")
                .referenceId(order.getId())
                .note("Auto-reset sesi QRIS kedaluwarsa: " + order.getOrderNumber())
                .build());
    }
}
