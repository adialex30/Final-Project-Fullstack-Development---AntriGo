package com.antrigo.backend.controller;

import com.antrigo.backend.dto.response.DashboardReportResponse;
import com.antrigo.backend.dto.response.LowStockResponse;
import com.antrigo.backend.service.ReportService;
import com.antrigo.backend.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@Tag(name = "Reports (Admin)", description = "Laporan otomatis: produk terlaris, stok rendah, jam tersibuk, pendapatan")
public class ReportController {

    private final ReportService reportService;
    private final StockService stockService;

    @GetMapping("/dashboard")
    public DashboardReportResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/low-stock")
    public List<LowStockResponse> lowStock() {
        return stockService.findLowStock();
    }
}
