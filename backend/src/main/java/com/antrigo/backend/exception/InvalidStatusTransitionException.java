package com.antrigo.backend.exception;

/** Dilempar saat transisi status pesanan tidak valid -> HTTP 409. */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
