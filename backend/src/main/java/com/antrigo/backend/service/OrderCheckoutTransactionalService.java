package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.*;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentMethod;
import com.antrigo.backend.domain.enums.PaymentStatus;
import com.antrigo.backend.domain.enums.StockMovementType;
import com.antrigo.backend.dto.request.CartItemRequest;
import com.antrigo.backend.dto.request.CheckoutRequest;
import com.antrigo.backend.dto.response.OrderResponse;
import com.antrigo.backend.exception.InsufficientStockException;
import com.antrigo.backend.exception.ResourceNotFoundException;
import com.antrigo.backend.payment.MidtransGatewayService;
import com.antrigo.backend.payment.QrisChargeResult;
import com.antrigo.backend.repository.*;
import com.antrigo.backend.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderCheckoutTransactionalService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final PaymentRepository paymentRepository;
    private final QueueService queueService;
    private final MidtransGatewayService midtransGatewayService;

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public OrderResponse execute(CheckoutRequest request) {

        List<Long> uniqueProductIds = request.items().stream()
                .map(CartItemRequest::productId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Long productId : uniqueProductIds) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + productId));
            if (!product.isActive()) {
                throw new InsufficientStockException("Produk '" + product.getName() + "' sedang tidak tersedia");
            }
            lockedProducts.put(productId, product);
        }

        Map<Long, Integer> requestedQtyPerProduct = new HashMap<>();
        for (CartItemRequest item : request.items()) {
            requestedQtyPerProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : requestedQtyPerProduct.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            if (product.getStock() < entry.getValue()) {
                throw new InsufficientStockException(
                        "Stok " + product.getName() + " tidak mencukupi (tersisa " + product.getStock() +
                                ", diminta " + entry.getValue() + ")");
            }
        }

        LocalDate businessDate = queueService.resolveBusinessDate();
        boolean isQris = request.paymentMethod() == PaymentMethod.QRIS;

        Integer queueNumber = null;
        int estimatedWait = 0;
        OrderStatus initialStatus = OrderStatus.AWAITING_PAYMENT;
        if (!isQris) {
            queueNumber = queueService.nextQueueNumber(businessDate);
            estimatedWait = queueService.estimateWaitMinutes(businessDate, queueNumber);
            initialStatus = OrderStatus.QUEUED;
        }

        Order order = Order.builder()
                .orderNumber(OrderNumberGenerator.generate(businessDate))
                .businessDate(businessDate)
                .queueNumber(queueNumber)
                .tableNumber(request.tableNumber())
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .status(initialStatus)
                .note(request.note())
                .estimatedWaitMinutes(estimatedWait)
                .subtotalAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemRequest itemReq : request.items()) {
            Product product = lockedProducts.get(itemReq.productId());
            ProductVariant variant = null;
            String variantName = null;
            BigDecimal unitPrice = product.getPrice();

            if (itemReq.variantId() != null) {
                variant = variantRepository.findById(itemReq.variantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Varian tidak ditemukan: " + itemReq.variantId()));
                if (!variant.getProduct().getId().equals(product.getId())) {
                    throw new IllegalArgumentException("Varian tidak sesuai dengan produk " + product.getName());
                }
                variantName = variant.getName();
                unitPrice = unitPrice.add(variant.getPriceDelta());
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .productNameSnapshot(product.getName())
                    .variantNameSnapshot(variantName)
                    .unitPriceSnapshot(unitPrice)
                    .quantity(itemReq.quantity())
                    .lineTotal(lineTotal)
                    .build();
            orderItems.add(orderItem);
        }

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        for (Map.Entry<Long, Integer> entry : requestedQtyPerProduct.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            product.setStock(product.getStock() - entry.getValue());
            productRepository.save(product);

            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .type(StockMovementType.OUT)
                    .quantityChange(-entry.getValue())
                    .referenceType("ORDER")
                    .referenceId(savedOrder.getId())
                    .note("Checkout " + savedOrder.getOrderNumber())
                    .build();
            stockMovementRepository.save(movement);
        }

        statusLogRepository.save(OrderStatusLog.builder()
                .order(savedOrder)
                .fromStatus(null)
                .toStatus(initialStatus.name())
                .note(isQris ? "Pesanan dibuat, menunggu pembayaran QRIS" : "Pesanan dibuat")
                .build());

        Payment.PaymentBuilder paymentBuilder = Payment.builder()
                .order(savedOrder)
                .method(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .paidAt(null);

        if (isQris) {
            QrisChargeResult charge = midtransGatewayService.createQrisCharge(savedOrder);
            paymentBuilder
                    .gatewayTransactionId(charge.transactionId())
                    .qrPayload(charge.qrPayload())
                    .expiresAt(charge.expiresAt());
        }
        Payment payment = paymentBuilder.build();
        paymentRepository.save(payment);

        return OrderResponse.from(savedOrder, payment);
    }
}
