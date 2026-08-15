package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.OrderStatusUpdateRequest;
import com.antrigo.backend.dto.response.KitchenBoardResponse;
import com.antrigo.backend.dto.response.OrderResponse;
import com.antrigo.backend.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Orders (Admin)", description = "Papan dapur & manajemen status pesanan")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/board")
    public List<KitchenBoardResponse> board() {
        return orderService.getKitchenBoard();
    }

    @PatchMapping("/{orderNumber}/status")
    public OrderResponse updateStatus(@PathVariable String orderNumber,
                                       @Valid @RequestBody OrderStatusUpdateRequest request,
                                       Authentication authentication) {
        return orderService.updateStatus(orderNumber, request, authentication.getName());
    }

    @PatchMapping("/{orderNumber}/payment/confirm")
    public OrderResponse confirmCashPayment(@PathVariable String orderNumber) {
        return orderService.confirmCashPayment(orderNumber);
    }
}
