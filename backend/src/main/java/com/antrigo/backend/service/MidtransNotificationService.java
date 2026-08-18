package com.antrigo.backend.service;

import com.antrigo.backend.dto.request.PaymentNotificationRequest;
import com.antrigo.backend.exception.PaymentGatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MidtransNotificationService {

    private static final Set<String> SETTLED_STATUSES = Set.of("settlement", "capture");
    private static final Set<String> FAILED_STATUSES = Set.of("deny", "cancel", "expire");

    private final OrderService orderService;

    @Value("${midtrans.server-key:}")
    private String serverKey;

    public void handle(PaymentNotificationRequest body) {
        if (!serverKey.isBlank() && !isSignatureValid(body)) {
            log.warn("Signature Midtrans tidak valid untuk order {} — cek MIDTRANS_SERVER_KEY", body.orderId());
            throw new PaymentGatewayException("Signature tidak valid");
        }

        String status = body.transactionStatus();
        if (SETTLED_STATUSES.contains(status) && "accept".equals(body.fraudStatus())) {
            log.info("Order {} settlement -> konfirmasi otomatis", body.orderId());
            orderService.confirmQrisPayment(body.orderId());
        } else if (FAILED_STATUSES.contains(status)) {
            log.info("Order {} status {} -> tandai gagal", body.orderId(), status);
            orderService.failQrisPayment(body.orderId());
        } else {
            log.info("Order {} status {} (diabaikan, bukan status final)", body.orderId(), status);
        }
    }

    private boolean isSignatureValid(PaymentNotificationRequest body) {
        try {
            String raw = body.orderId() + body.statusCode() + body.grossAmount() + serverKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().equalsIgnoreCase(body.signatureKey());
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}