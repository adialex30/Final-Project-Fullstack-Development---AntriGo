package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.CheckoutRequest;
import com.antrigo.backend.dto.response.OrderResponse;
import com.antrigo.backend.dto.response.OrderStatusResponse;
import com.antrigo.backend.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders (Customer)", description = "Checkout, cek status, dan pembatalan pesanan pelanggan")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse detail(@PathVariable String orderNumber) {
        return orderService.getByOrderNumber(orderNumber);
    }

    @GetMapping("/{orderNumber}/status")
    public OrderStatusResponse status(@PathVariable String orderNumber) {
        return orderService.getStatus(orderNumber);
    }

    @PostMapping("/{orderNumber}/payments/qris/confirm")
    public OrderResponse confirmQrisPayment(@PathVariable String orderNumber) {
        return orderService.confirmQrisPayment(orderNumber);
    }

    @PatchMapping("/{orderNumber}/cancel")
    public OrderResponse cancel(@PathVariable String orderNumber, @RequestParam(required = false) String reason) {
        return orderService.cancel(orderNumber, reason);
    }
}