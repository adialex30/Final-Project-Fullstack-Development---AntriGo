package com.antrigo.backend.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void awaitingPaymentCanMoveToQueuedOrCancelled() {
        assertTrue(OrderStatus.AWAITING_PAYMENT.canTransitionTo(OrderStatus.QUEUED));
        assertTrue(OrderStatus.AWAITING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.AWAITING_PAYMENT.canTransitionTo(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.AWAITING_PAYMENT.canTransitionTo(OrderStatus.READY));
    }

    @Test
    void queuedCanMoveToProcessingOrCancelled() {
        assertTrue(OrderStatus.QUEUED.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.QUEUED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.QUEUED.canTransitionTo(OrderStatus.READY));
        assertFalse(OrderStatus.QUEUED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void readyCanOnlyMoveToCompleted() {
        assertTrue(OrderStatus.READY.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.QUEUED));
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.COMPLETED.canTransitionTo(target));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
        }
    }
}
