package com.antrigo.backend.exception;

public class BadCredentialsExceptionCustom extends RuntimeException {
    public BadCredentialsExceptionCustom(String message) {
        super(message);
    }
}
