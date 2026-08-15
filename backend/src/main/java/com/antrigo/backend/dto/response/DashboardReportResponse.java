package com.antrigo.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardReportResponse(
        BigDecimal revenueToday,
        long ordersToday,
        long ordersActiveNow,
        List<TopProductResponse> topProducts,
        List<LowStockResponse> lowStockProducts,
        List<BusiestHourResponse> busiestHours
) {}
