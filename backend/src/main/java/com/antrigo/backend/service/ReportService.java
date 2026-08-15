package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.enums.OrderStatus;
import com.antrigo.backend.dto.response.*;
import com.antrigo.backend.repository.OrderItemRepository;
import com.antrigo.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReportService — laporan otomatis untuk keputusan bisnis pemilik warung: produk terlaris,
 * stok rendah, jam tersibuk, tren pendapatan. Hasil di-cache di Redis (TTL pendek, 30 detik)
 * dan otomatis di-evict tiap ada order/stock baru (lihat evictProductAndReportCaches di OrderService).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockService stockService;

    @Cacheable(value = "reports", key = "'dashboard-' + T(java.time.LocalDate).now()")
    public DashboardReportResponse dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime to = LocalDateTime.of(today, LocalTime.MAX);

        List<Order> ordersToday = orderRepository.findBoard(today,
                List.of(OrderStatus.QUEUED, OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.COMPLETED));

        BigDecimal revenueToday = ordersToday.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeNow = orderRepository.countByBusinessDateAndStatusIn(today,
                List.of(OrderStatus.QUEUED, OrderStatus.PROCESSING));

        List<TopProductResponse> topProducts = orderItemRepository.findTopProducts(from, to).stream()
                .limit(5)
                .map(p -> new TopProductResponse(p.getProductId(), p.getProductName(), p.getTotalQty(), p.getTotalRevenue()))
                .collect(Collectors.toList());

        List<LowStockResponse> lowStock = stockService.findLowStock();

        List<BusiestHourResponse> busiestHours = ordersToday.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new BusiestHourResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(BusiestHourResponse::hour))
                .collect(Collectors.toList());

        return new DashboardReportResponse(revenueToday, ordersToday.size(), activeNow, topProducts, lowStock, busiestHours);
    }
}
