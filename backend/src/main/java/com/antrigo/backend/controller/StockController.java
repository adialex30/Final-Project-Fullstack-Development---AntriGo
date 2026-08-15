package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.StockAdjustmentRequest;
import com.antrigo.backend.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/stock")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Stock (Admin)", description = "Penyesuaian stok manual — selalu tercatat di ledger")
public class StockController {

    private final StockService stockService;

    @PostMapping("/{productId}/adjust")
    public void adjust(@PathVariable Long productId,
                        @Valid @RequestBody StockAdjustmentRequest request,
                        Authentication authentication) {
        stockService.adjust(productId, request, authentication.getName());
    }
}
