package com.antrigo.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentNotificationRequest(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("status_code") String statusCode,
        @JsonProperty("gross_amount") String grossAmount,
        @JsonProperty("signature_key") String signatureKey,
        @JsonProperty("transaction_status") String transactionStatus,
        @JsonProperty("fraud_status") String fraudStatus,
        @JsonProperty("payment_type") String paymentType
) {}