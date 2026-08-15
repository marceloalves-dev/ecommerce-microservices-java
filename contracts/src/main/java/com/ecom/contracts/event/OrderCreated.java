package com.ecom.contracts.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreated(
        UUID orderId,
        UUID customerId,
        List<Item> items,
        BigDecimal totalAmount,
        String currency,
        String reservationId) {
    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }
}
