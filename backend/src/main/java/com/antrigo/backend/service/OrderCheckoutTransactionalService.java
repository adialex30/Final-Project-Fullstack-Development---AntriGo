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
import com.antrigo.backend.repository.*;
import com.antrigo.backend.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Bagian tersulit dari AntriGo: satu checkout = satu transaksi database yang harus benar
 * di bawah concurrency tinggi (banyak pelanggan checkout bersamaan, jam makan siang).
 *
 * Strategi:
 *  1. Kumpulkan productId unik dari keranjang, urutkan ASCENDING, lalu kunci baris produk satu per
 *     satu dengan PESSIMISTIC_WRITE (SELECT ... FOR UPDATE). Urutan menaik ini yang mencegah
 *     deadlock: kalau dua checkout sama-sama memesan produk A dan B tapi mengunci dengan urutan
 *     berbeda, keduanya bisa saling menunggu selamanya. Mengunci selalu dari id terkecil membuat
 *     kedua transaksi antre di urutan yang sama, bukan saling silang.
 *  2. Validasi ulang stok SETELAH baris terkunci (bukan sebelumnya) — supaya keputusan "cukup/tidak"
 *     dibuat atas data yang benar-benar terbaru, bukan data basi yang dibaca sebelum lock didapat.
 *  3. Harga dihitung ulang dari data produk di server, tidak pernah dari payload.
 *  4. Nomor antrean digenerate di transaksi yang sama; unique index (business_date, queue_number)
 *     adalah jaring pengaman terakhir — kalau tetap tabrakan, OrderService di lapisan luar akan
 *     retry dengan transaksi baru.
 */
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

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public OrderResponse execute(CheckoutRequest request) {

        // --- 1. Kunci produk secara berurutan menurut id, cegah deadlock ---
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

        // --- 2. Validasi stok atas data yang sudah terkunci + hitung total qty per produk ---
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

        // --- 3. Bangun order + item dengan harga dihitung ulang di server ---
        LocalDate businessDate = queueService.resolveBusinessDate();
        int queueNumber = queueService.nextQueueNumber(businessDate);
        int estimatedWait = queueService.estimateWaitMinutes(businessDate, queueNumber);

        Order order = Order.builder()
                .orderNumber(OrderNumberGenerator.generate(businessDate))
                .businessDate(businessDate)
                .queueNumber(queueNumber)
                .tableNumber(request.tableNumber())
                .status(OrderStatus.QUEUED)
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
        order.setTotalAmount(subtotal); // tidak ada pajak/service charge di versi ini
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order); // cascade menyimpan order_items

        // --- 4. Kurangi stok (cache) + catat ledger stock_movements ---
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

        // --- 5. Audit trail status + payment ---
        statusLogRepository.save(OrderStatusLog.builder()
                .order(savedOrder)
                .fromStatus(null)
                .toStatus(OrderStatus.QUEUED.name())
                .note("Pesanan dibuat")
                .build());

        boolean instantPay = request.paymentMethod() == PaymentMethod.QRIS;
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(request.paymentMethod())
                .status(instantPay ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .amount(order.getTotalAmount())
                .paidAt(instantPay ? LocalDateTime.now() : null)
                .build();
        paymentRepository.save(payment);

        return OrderResponse.from(savedOrder);
    }
}
