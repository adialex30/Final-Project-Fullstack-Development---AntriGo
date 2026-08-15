package com.antrigo.backend.repository;

import com.antrigo.backend.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT oi.product.id as productId, oi.productNameSnapshot as productName,
               SUM(oi.quantity) as totalQty, SUM(oi.lineTotal) as totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status <> 'CANCELLED' AND o.createdAt BETWEEN :from AND :to
        GROUP BY oi.product.id, oi.productNameSnapshot
        ORDER BY totalQty DESC
    """)
    List<TopProductProjection> findTopProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface TopProductProjection {
        Long getProductId();
        String getProductName();
        Long getTotalQty();
        java.math.BigDecimal getTotalRevenue();
    }
}
