package com.ecom.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Projeção leve usada na listagem; não materializa os itens do pedido. */
public record OrderSummary(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        CurrencyCode currency,
        Instant createdAt,
        Instant updatedAt) {
}
