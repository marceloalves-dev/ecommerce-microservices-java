package com.ecom.order.api.dto;

import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderStatus;
import com.ecom.order.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        CurrencyCode currency,
        String cancellationReason,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.items().stream()
                .map(i -> new Item(i.sku(), i.quantity(), i.unitPrice()))
                .toList();
        return new OrderResponse(
                order.id(),
                order.customerId(),
                order.status(),
                order.totalAmount(),
                order.currency(),
                order.cancellationReason(),
                items,
                order.createdAt(),
                order.updatedAt());
    }
}
