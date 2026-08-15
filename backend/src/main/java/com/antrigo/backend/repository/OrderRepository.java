package com.antrigo.backend.repository;

import com.antrigo.backend.domain.entity.Order;
import com.antrigo.backend.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT COALESCE(MAX(o.queueNumber), 0) FROM Order o WHERE o.businessDate = :date")
    int findMaxQueueNumberForDate(@Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.businessDate = :date AND o.status IN ('QUEUED','PROCESSING')
        AND o.queueNumber < :queueNumber
    """)
    long countActiveAheadOf(@Param("date") LocalDate date, @Param("queueNumber") int queueNumber);

    @Query("""
        SELECT o FROM Order o
        WHERE o.businessDate = :date AND o.status IN :statuses
        ORDER BY o.queueNumber ASC
    """)
    List<Order> findBoard(@Param("date") LocalDate date, @Param("statuses") List<OrderStatus> statuses);

    long countByBusinessDateAndStatusIn(LocalDate date, List<OrderStatus> statuses);
}
