package com.antrigo.backend.controller;

import com.antrigo.backend.dto.response.DashboardReportResponse;
import com.antrigo.backend.dto.response.LowStockResponse;
import com.antrigo.backend.service.ReportService;
import com.antrigo.backend.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public DashboardReportResponse dashboard(Authentication authentication) {
        DashboardReportResponse report = reportService.dashboard();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return new DashboardReportResponse(
                    null,
                    report.ordersToday(),
                    report.ordersActiveNow(),
                    report.topProducts(),
                    report.lowStockProducts(),
                    report.busiestHours()
            );
        }
        return report;
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LowStockResponse> lowStock() {
        return stockService.findLowStock();
    }
}