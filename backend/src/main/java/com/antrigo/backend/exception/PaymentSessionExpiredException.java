package com.antrigo.backend.exception;

public class PaymentSessionExpiredException extends RuntimeException {
    public PaymentSessionExpiredException(String message) {
        super(message);
    }
}
