package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.*;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentStatus;
import com.antrigo.backend.domain.enums.StockMovementType;
import com.antrigo.backend.dto.request.CheckoutRequest;
import com.antrigo.backend.dto.request.OrderStatusUpdateRequest;
import com.antrigo.backend.dto.response.KitchenBoardResponse;
import com.antrigo.backend.dto.response.OrderItemResponse;
import com.antrigo.backend.dto.response.OrderResponse;
import com.antrigo.backend.dto.response.OrderStatusResponse;
import com.antrigo.backend.exception.InvalidStatusTransitionException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final int MAX_CHECKOUT_RETRIES = 5;
    private static final List<OrderStatus> BOARD_STATUSES = List.of(
            OrderStatus.QUEUED, OrderStatus.PROCESSING, OrderStatus.READY);

    private final OrderCheckoutTransactionalService checkoutTransactionalService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final QueueService queueService;
    private final CacheManager cacheManager;

    /**
     * Retry di lapisan ini (bukan di dalam transaksi) karena begitu DataIntegrityViolationException
     * terlempar, transaksi Spring sudah ditandai rollback-only — transaksi lama tidak bisa dipakai
     * lagi. Setiap percobaan membuka transaksi REQUIRES_NEW yang baru sepenuhnya.
     */
    public OrderResponse checkout(CheckoutRequest request) {
        DataIntegrityViolationException lastError = null;
        for (int attempt = 1; attempt <= MAX_CHECKOUT_RETRIES; attempt++) {
            try {
                OrderResponse response = checkoutTransactionalService.execute(request);
                evictProductAndReportCaches();
                return response;
            } catch (DataIntegrityViolationException e) {
                lastError = e;
                log.warn("Checkout attempt {} gagal karena collision, retry...", attempt);
            }
        }
        throw new IllegalStateException("Gagal memproses pesanan setelah beberapa percobaan, coba lagi", lastError);
    }

    public OrderResponse getByOrderNumber(String orderNumber) {
        return OrderResponse.from(findOrderOrThrow(orderNumber));
    }

    /** Endpoint ringan untuk polling (TanStack Query refetchInterval) — estimasi dihitung ulang live. */
    public OrderStatusResponse getStatus(String orderNumber) {
        Order order = findOrderOrThrow(orderNumber);
        if (order.getStatus() == OrderStatus.QUEUED || order.getStatus() == OrderStatus.PROCESSING) {
            int liveEstimate = queueService.estimateWaitMinutes(order.getBusinessDate(), order.getQueueNumber());
            order.setEstimatedWaitMinutes(liveEstimate);
        } else {
            order.setEstimatedWaitMinutes(0);
        }
        return OrderStatusResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(String orderNumber, String reason) {
        Order order = findOrderOrThrow(orderNumber);
        transitionStatus(order, OrderStatus.CANCELLED, reason, null);
        reverseStockForOrder(order, "Pembatalan " + order.getOrderNumber());
        evictProductAndReportCaches();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(String orderNumber, OrderStatusUpdateRequest request, String actorEmail) {
        Order order = findOrderOrThrow(orderNumber);
        User actor = actorEmail == null ? null : userRepository.findByEmailAndActiveTrue(actorEmail).orElse(null);
        transitionStatus(order, request.status(), request.note(), actor);
        if (request.status() == OrderStatus.CANCELLED) {
            reverseStockForOrder(order, "Dibatalkan admin: " + order.getOrderNumber());
        }
        evictProductAndReportCaches();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirmCashPayment(String orderNumber) {
        Order order = findOrderOrThrow(orderNumber);
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment untuk pesanan ini tidak ditemukan"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return OrderResponse.from(order);
    }

    /** Papan dapur admin — pesanan hari ini yang masih aktif, berurutan menurut nomor antrean. */
    public List<KitchenBoardResponse> getKitchenBoard() {
        var businessDate = queueService.resolveBusinessDate();
        return orderRepository.findBoard(businessDate, BOARD_STATUSES).stream()
                .map(this::toBoardResponse)
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private Order findOrderOrThrow(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan: " + orderNumber));
    }

    private void transitionStatus(Order order, OrderStatus next, String note, User actor) {
        if (!order.getStatus().canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(
                    "Tidak bisa mengubah status dari " + order.getStatus() + " ke " + next);
        }
        String from = order.getStatus().name();
        order.setStatus(next);
        orderRepository.save(order);

        statusLogRepository.save(OrderStatusLog.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(next.name())
                .changedBy(actor)
                .note(note)
                .build());
    }

    /** Kembalikan stok yang sudah dikurangi saat checkout — dicatat sebagai movement baru, bukan menghapus riwayat lama. */
    private void reverseStockForOrder(Order order, String note) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);

            stockMovementRepository.save(StockMovement.builder()
                    .product(product)
                    .type(StockMovementType.CANCELLATION_REVERSAL)
                    .quantityChange(item.getQuantity())
                    .referenceType("ORDER")
                    .referenceId(order.getId())
                    .note(note)
                    .build());
        }
    }

    private KitchenBoardResponse toBoardResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());
        int waitingMinutes = (int) Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes();
        return new KitchenBoardResponse(
                order.getOrderNumber(), order.getQueueNumber(), order.getTableNumber(),
                order.getStatus(), items, order.getCreatedAt(), Math.max(waitingMinutes, 0));
    }

    private void evictProductAndReportCaches() {
        if (cacheManager.getCache("products") != null) cacheManager.getCache("products").clear();
        if (cacheManager.getCache("reports") != null) cacheManager.getCache("reports").clear();
    }
}
