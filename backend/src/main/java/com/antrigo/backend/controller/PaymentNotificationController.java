package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.PaymentNotificationRequest;
import com.antrigo.backend.service.MidtransNotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/midtrans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Notification", description = "Webhook Midtrans")
public class PaymentNotificationController {

    private final MidtransNotificationService notificationService;

    @PostMapping("/notification")
    public void notification(@RequestBody PaymentNotificationRequest body) {
        log.info("Notifikasi Midtrans diterima: order={} status={} fraud={}",
                body.orderId(), body.transactionStatus(), body.fraudStatus());
        notificationService.handle(body);
    }
}