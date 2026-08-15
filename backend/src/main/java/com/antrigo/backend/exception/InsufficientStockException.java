package com.antrigo.backend.exception;

/** Dilempar saat stok tidak mencukupi di dalam transaksi checkout -> HTTP 422. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
