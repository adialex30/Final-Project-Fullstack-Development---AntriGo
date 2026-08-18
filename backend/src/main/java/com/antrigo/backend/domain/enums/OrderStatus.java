package com.antrigo.backend.domain.enums;

import java.util.List;
import java.util.Map;

public enum OrderStatus {
    AWAITING_PAYMENT, QUEUED, PROCESSING, READY, COMPLETED, CANCELLED;

    private static final Map<OrderStatus, List<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            AWAITING_PAYMENT, List.of(QUEUED, CANCELLED),
            QUEUED, List.of(PROCESSING, CANCELLED),
            PROCESSING, List.of(READY, CANCELLED),
            READY, List.of(COMPLETED),
            COMPLETED, List.of(),
            CANCELLED, List.of()
    );

    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, List.of()).contains(next);
    }
}
