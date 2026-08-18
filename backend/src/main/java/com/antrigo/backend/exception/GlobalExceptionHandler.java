// package com.antrigo.backend.exception;

// import com.antrigo.backend.dto.response.ApiError;
// import jakarta.servlet.http.HttpServletRequest;
// import org.springframework.dao.DataIntegrityViolationException;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.authentication.BadCredentialsException;
// import org.springframework.validation.FieldError;
// import org.springframework.web.bind.MethodArgumentNotValidException;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import lombok.extern.slf4j.Slf4j;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.stream.Collectors;

// /**
//  * Global exception handler — menyeragamkan seluruh error response API ke satu format (ApiError).
//  * Pemetaan HTTP status mengikuti kontrak di technical depth:
//  *   201 dibuat, 403 role tidak berhak, 409 transisi status tidak valid, 410 sesi pembayaran QRIS
//  *   kedaluwarsa, 422 stok tidak mencukupi.
//  */
// @RestControllerAdvice
// @Slf4j
// public class GlobalExceptionHandler {

//     @ExceptionHandler(ResourceNotFoundException.class)
//     public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
//         return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
//     }

//     @ExceptionHandler(InsufficientStockException.class)
//     public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
//         return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
//     }

//     @ExceptionHandler(InvalidStatusTransitionException.class)
//     public ResponseEntity<ApiError> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest req) {
//         return build(HttpStatus.CONFLICT, ex.getMessage(), req);
//     }

//     @ExceptionHandler(PaymentSessionExpiredException.class)
//     public ResponseEntity<ApiError> handlePaymentSessionExpired(PaymentSessionExpiredException ex, HttpServletRequest req) {
//         return build(HttpStatus.GONE, ex.getMessage(), req);
//     }

//     @ExceptionHandler(DuplicateResourceException.class)
//     public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
//         return build(HttpStatus.CONFLICT, ex.getMessage(), req);
//     }

//     @ExceptionHandler(BadCredentialsExceptionCustom.class)
//     public ResponseEntity<ApiError> handleBadCredentialsCustom(BadCredentialsExceptionCustom ex, HttpServletRequest req) {
//         return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
//     }

//     @ExceptionHandler(BadCredentialsException.class)
//     public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
//         return build(HttpStatus.UNAUTHORIZED, "Email atau password salah", req);
//     }

//     @ExceptionHandler(AccessDeniedException.class)
//     public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
//         return build(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk aksi ini", req);
//     }

//     @ExceptionHandler(DataIntegrityViolationException.class)
//     public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
//         return build(HttpStatus.CONFLICT, "Data melanggar constraint unik atau referensial", req);
//     }

//     @ExceptionHandler(MethodArgumentNotValidException.class)
//     public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
//         Map<String, String> fieldErrors = new HashMap<>();
//         for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
//             fieldErrors.put(fe.getField(), fe.getDefaultMessage());
//         }
//         ApiError error = ApiError.ofValidation(
//                 HttpStatus.BAD_REQUEST.value(), "Bad Request",
//                 "Validasi gagal pada satu atau lebih field", req.getRequestURI(), fieldErrors);
//         return ResponseEntity.badRequest().body(error);
//     }

//     @ExceptionHandler(IllegalArgumentException.class)
//     public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
//         return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
//     }

//     @ExceptionHandler(Exception.class)
//         public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
//         log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
//         return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan internal pada server", req);
//     }

//     private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
//         ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
//         return ResponseEntity.status(status).body(error);
//     }
// }

package com.antrigo.backend.exception;

import com.antrigo.backend.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(InvalidStatusTransitionException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(PaymentSessionExpiredException.class)
    public ResponseEntity<ApiError> handlePaymentSessionExpired(PaymentSessionExpiredException ex, HttpServletRequest req) {
        return build(HttpStatus.GONE, ex.getMessage(), req);
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiError> handlePaymentGateway(PaymentGatewayException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsExceptionCustom.class)
    public ResponseEntity<ApiError> handleBadCredentialsCustom(BadCredentialsExceptionCustom ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Email atau password salah", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk aksi ini", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Data melanggar constraint unik atau referensial", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiError error = ApiError.ofValidation(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Validasi gagal pada satu atau lebih field", req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        // Sebelumnya exception di sini TIDAK pernah tercatat sama sekali di log — client cuma
        // dapat pesan generik, server tidak menyisakan jejak apa pun untuk debugging. Ditambahkan
        // supaya stack trace asli selalu muncul di `docker compose logs backend`.
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan internal pada server", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}