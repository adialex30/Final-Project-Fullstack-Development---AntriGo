package com.antrigo.backend.repository;

import com.antrigo.backend.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = 'PENDING' AND p.expiresAt IS NOT NULL AND p.expiresAt < :now
    """)
    List<Payment> findExpiredPending(@Param("now") LocalDateTime now);
}
