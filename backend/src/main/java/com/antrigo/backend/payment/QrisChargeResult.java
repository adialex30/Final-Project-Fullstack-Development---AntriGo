package com.antrigo.backend.payment;

import java.time.LocalDateTime;

public record QrisChargeResult(
        String transactionId,
        String qrPayload,
        LocalDateTime expiresAt
) {}
