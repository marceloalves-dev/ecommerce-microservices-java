package com.ecom.order.domain.model;

/**
 * Estados da saga do pedido. Owner: order-service.
 *
 * <pre>
 * PENDING -> AWAITING_PAYMENT -> CONFIRMED
 *                             -> CANCELLED
 *         -> REJECTED
 * </pre>
 */
public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    REJECTED
}
