package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.Payment;
import com.antrigo.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentExpiryHelper expiryHelper;

    @Scheduled(fixedDelayString = "${app.qris.expiry-sweep-ms:60000}")
    @Transactional
    public void sweepExpiredPayments() {
        List<Payment> expired = paymentRepository.findExpiredPending(LocalDateTime.now());
        for (Payment payment : expired) {
            expiryHelper.expireIfDue(payment.getOrder(), payment);
        }
        if (!expired.isEmpty()) {
            log.info("[PaymentExpiryScheduler] {} sesi pembayaran QRIS kedaluwarsa direset otomatis.", expired.size());
        }
    }
}
