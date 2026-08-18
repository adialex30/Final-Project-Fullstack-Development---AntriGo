package com.antrigo.backend.seeder;

import com.antrigo.backend.domain.entity.*;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.domain.enums.PaymentMethod;
import com.antrigo.backend.domain.enums.PaymentStatus;
import com.antrigo.backend.domain.enums.Role;
import com.antrigo.backend.domain.enums.StockMovementType;
import com.antrigo.backend.repository.*;
import com.antrigo.backend.util.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Jakarta");
    private static final int HISTORY_DAYS = 10;
    private static final Random RANDOM = new Random(42);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final JdbcTemplate jdbcTemplate;

    private final List<TimestampPatch> patches = new ArrayList<>();

    private record TimestampPatch(String table, String column, Long id, LocalDateTime value) {}

    @Override
    @Transactional
    public void run(String... args) {
        long existingOrders = orderRepository.count();
        if (existingOrders > 0) {
            log.info("[DemoDataSeeder] Dilewati — sudah ada {} pesanan di database.", existingOrders);
            return;
        }

        List<Product> products = productRepository.findAll().stream()
                .filter(Product::isActive)
                .toList();
        if (products.isEmpty()) {
            log.warn("[DemoDataSeeder] Tidak ada produk aktif — pastikan Flyway V2__seed_data.sql sudah jalan. Seeder dibatalkan.");
            return;
        }

        User staffActor = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STAFF || u.getRole() == Role.ADMIN)
                .findFirst()
                .orElse(null);

        Map<Long, Integer> stockPool = new HashMap<>();
        for (Product p : products) {
            stockPool.put(p.getId(), p.getStock());
        }

        LocalDate today = LocalDate.now(STORE_ZONE);
        LocalDateTime now = LocalDateTime.now(STORE_ZONE);
        int totalOrders = 0;
        int totalCancelled = 0;

        for (int dayOffset = HISTORY_DAYS - 1; dayOffset >= 0; dayOffset--) {
            LocalDate businessDate = today.minusDays(dayOffset);
            boolean isToday = dayOffset == 0;
            int ordersToday = isToday ? (6 + RANDOM.nextInt(6)) : (8 + RANDOM.nextInt(10));

            List<int[]> timeSlots = new ArrayList<>();
            for (int i = 0; i < ordersToday; i++) {
                timeSlots.add(new int[]{weightedHour(), RANDOM.nextInt(60)});
            }
            timeSlots.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);

            int queueNumber = 1;
            for (int i = 0; i < timeSlots.size(); i++) {
                List<OrderItemDraft> lines = buildLines(products, stockPool);
                if (lines.isEmpty()) {
                    break;
                }

                LocalDateTime createdAt = businessDate.atTime(timeSlots.get(i)[0], timeSlots.get(i)[1]);
                if (isToday && createdAt.isAfter(now)) {
                    createdAt = now.minusMinutes(RANDOM.nextInt(90));
                }

                OrderStatus finalStatus = pickStatus(isToday, i, timeSlots.size());
                boolean cancelled = finalStatus == OrderStatus.CANCELLED;

                Order order = Order.builder()
                        .orderNumber(OrderNumberGenerator.generate(businessDate))
                        .businessDate(businessDate)
                        .queueNumber(queueNumber++)
                        .tableNumber(RANDOM.nextInt(10) < 8 ? String.valueOf(1 + RANDOM.nextInt(20)) : null)
                        .status(finalStatus)
                        .subtotalAmount(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)
                        .estimatedWaitMinutes(4 * lines.size())
                        .build();

                BigDecimal total = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();
                for (OrderItemDraft d : lines) {
                    total = total.add(d.lineTotal());
                    orderItems.add(OrderItem.builder()
                            .order(order)
                            .product(d.product())
                            .variant(d.variant())
                            .productNameSnapshot(d.product().getName())
                            .variantNameSnapshot(d.variant() != null ? d.variant().getName() : null)
                            .unitPriceSnapshot(d.unitPrice())
                            .quantity(d.quantity())
                            .lineTotal(d.lineTotal())
                            .build());
                }
                order.setSubtotalAmount(total);
                order.setTotalAmount(total);
                order.setItems(orderItems);

                Order savedOrder = orderRepository.save(order);
                patches.add(new TimestampPatch("orders", "created_at", savedOrder.getId(), createdAt));

                LocalDateTime cursor = createdAt;
                for (OrderStatus step : statusChainFor(finalStatus)) {
                    cursor = cursor.plusMinutes(2 + RANDOM.nextInt(4));
                }
                patches.add(new TimestampPatch("orders", "updated_at", savedOrder.getId(), cursor));

                for (OrderItem item : savedOrder.getItems()) {
                    patches.add(new TimestampPatch("order_items", "created_at", item.getId(), createdAt));
                }

                LocalDateTime movementTime = createdAt;
                for (OrderItemDraft d : lines) {
                    StockMovement out = stockMovementRepository.save(StockMovement.builder()
                            .product(d.product())
                            .type(StockMovementType.OUT)
                            .quantityChange(-d.quantity())
                            .referenceType("ORDER")
                            .referenceId(savedOrder.getId())
                            .note("Checkout " + savedOrder.getOrderNumber())
                            .build());
                    patches.add(new TimestampPatch("stock_movements", "created_at", out.getId(), movementTime));

                    if (cancelled) {
                        LocalDateTime reversalTime = movementTime.plusMinutes(3 + RANDOM.nextInt(10));
                        StockMovement reversal = stockMovementRepository.save(StockMovement.builder()
                                .product(d.product())
                                .type(StockMovementType.CANCELLATION_REVERSAL)
                                .quantityChange(d.quantity())
                                .referenceType("ORDER")
                                .referenceId(savedOrder.getId())
                                .note("Pembatalan " + savedOrder.getOrderNumber())
                                .build());
                        patches.add(new TimestampPatch("stock_movements", "created_at", reversal.getId(), reversalTime));
                        stockPool.merge(d.product().getId(), d.quantity(), Integer::sum);
                    }
                }

                String prev = null;
                LocalDateTime logTime = createdAt;
                for (OrderStatus step : statusChainFor(finalStatus)) {
                    User actor = step == OrderStatus.QUEUED ? null : staffActor;
                    OrderStatusLog logEntry = statusLogRepository.save(OrderStatusLog.builder()
                            .order(savedOrder)
                            .fromStatus(prev)
                            .toStatus(step.name())
                            .changedBy(actor)
                            .note(step == OrderStatus.QUEUED ? "Pesanan dibuat" : null)
                            .build());
                    patches.add(new TimestampPatch("order_status_logs", "created_at", logEntry.getId(), logTime));
                    prev = step.name();
                    logTime = logTime.plusMinutes(2 + RANDOM.nextInt(4));
                }

                PaymentMethod method = RANDOM.nextBoolean() ? PaymentMethod.QRIS : PaymentMethod.CASH;
                PaymentStatus paymentStatus;
                LocalDateTime paidAt = null;
                if (cancelled) {
                    paymentStatus = RANDOM.nextInt(10) < 6 ? PaymentStatus.REFUNDED : PaymentStatus.FAILED;
                } else if (method == PaymentMethod.QRIS) {
                    paymentStatus = PaymentStatus.PAID;
                    paidAt = createdAt;
                } else {
                    boolean unconfirmed = finalStatus == OrderStatus.QUEUED && RANDOM.nextBoolean();
                    paymentStatus = unconfirmed ? PaymentStatus.PENDING : PaymentStatus.PAID;
                    paidAt = unconfirmed ? null : createdAt.plusMinutes(1);
                }
                Payment payment = paymentRepository.save(Payment.builder()
                        .order(savedOrder)
                        .method(method)
                        .status(paymentStatus)
                        .amount(total)
                        .paidAt(paidAt)
                        .build());
                patches.add(new TimestampPatch("payments", "created_at", payment.getId(), createdAt));
                if (paidAt != null) {
                    patches.add(new TimestampPatch("payments", "paid_at", payment.getId(), paidAt));
                }

                totalOrders++;
                if (cancelled) totalCancelled++;
            }
        }

        for (Product p : products) {
            p.setStock(stockPool.get(p.getId()));
            productRepository.save(p);
        }

        applyTimestampPatches();
        upsertSeedMarker(now);

        log.info("[DemoDataSeeder] Selesai: {} pesanan dibuat mencakup {} hari terakhir ({} dibatalkan).",
                totalOrders, HISTORY_DAYS, totalCancelled);
    }

    private record OrderItemDraft(Product product, ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    private int weightedHour() {
        int roll = RANDOM.nextInt(100);
        if (roll < 45) return 11 + RANDOM.nextInt(4);
        if (roll < 80) return 17 + RANDOM.nextInt(4);
        return 8 + RANDOM.nextInt(13);
    }

    private OrderStatus pickStatus(boolean isToday, int index, int totalToday) {
        if (!isToday) {
            return RANDOM.nextInt(100) < 6 ? OrderStatus.CANCELLED : OrderStatus.COMPLETED;
        }
        double progress = totalToday <= 1 ? 1.0 : (double) index / (totalToday - 1);
        if (progress < 0.5) {
            return RANDOM.nextInt(100) < 4 ? OrderStatus.CANCELLED : OrderStatus.COMPLETED;
        } else if (progress < 0.7) {
            return OrderStatus.READY;
        } else if (progress < 0.88) {
            return OrderStatus.PROCESSING;
        } else {
            return OrderStatus.QUEUED;
        }
    }

    private List<OrderStatus> statusChainFor(OrderStatus finalStatus) {
        return switch (finalStatus) {
            case QUEUED -> List.of(OrderStatus.QUEUED);
            case PROCESSING -> List.of(OrderStatus.QUEUED, OrderStatus.PROCESSING);
            case READY -> List.of(OrderStatus.QUEUED, OrderStatus.PROCESSING, OrderStatus.READY);
            case COMPLETED -> List.of(OrderStatus.QUEUED, OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.COMPLETED);
            case CANCELLED -> List.of(OrderStatus.QUEUED, OrderStatus.CANCELLED);
            default -> List.of(OrderStatus.QUEUED); // <-- Tambahkan baris ini untuk mencakup sisa/nilai lain
        };
    }

    private List<OrderItemDraft> buildLines(List<Product> products, Map<Long, Integer> stockPool) {
        List<Product> available = new ArrayList<>(products.stream()
                .filter(p -> stockPool.getOrDefault(p.getId(), 0) > 0)
                .toList());
        if (available.isEmpty()) return List.of();

        Collections.shuffle(available, RANDOM);
        int lineCount = 1 + RANDOM.nextInt(Math.min(3, available.size()));
        List<OrderItemDraft> lines = new ArrayList<>();

        for (int i = 0; i < lineCount && i < available.size(); i++) {
            Product product = available.get(i);
            int remaining = stockPool.getOrDefault(product.getId(), 0);
            if (remaining <= 0) continue;

            int qty = 1 + RANDOM.nextInt(Math.min(2, remaining));
            ProductVariant variant = pickVariant(product);
            BigDecimal unitPrice = product.getPrice().add(variant != null ? variant.getPriceDelta() : BigDecimal.ZERO);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

            lines.add(new OrderItemDraft(product, variant, qty, unitPrice, lineTotal));
            stockPool.put(product.getId(), remaining - qty);
        }
        return lines;
    }

    private ProductVariant pickVariant(Product product) {
        List<ProductVariant> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) return null;
        List<ProductVariant> active = variants.stream().filter(ProductVariant::isActive).toList();
        if (active.isEmpty() || RANDOM.nextInt(100) >= 70) return null;
        return active.get(RANDOM.nextInt(active.size()));
    }

    private void applyTimestampPatches() {
        for (TimestampPatch p : patches) {
            jdbcTemplate.update(
                    "UPDATE " + p.table() + " SET " + p.column() + " = ? WHERE id = ?",
                    Timestamp.valueOf(p.value()), p.id());
        }
        patches.clear();
    }

    private void upsertSeedMarker(LocalDateTime seededAt) {
        StoreSetting marker = storeSettingRepository.findBySettingKey("demo_seeded_at")
                .orElse(StoreSetting.builder().settingKey("demo_seeded_at").build());
        marker.setSettingValue(seededAt.toString());
        storeSettingRepository.save(marker);
    }
}