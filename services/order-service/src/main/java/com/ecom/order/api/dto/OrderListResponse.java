package com.ecom.order.api.dto;

import com.ecom.order.application.port.in.GetOrderUseCase.OrderSlice;
import com.ecom.order.domain.model.CurrencyCode;
import com.ecom.order.domain.model.OrderStatus;
import com.ecom.order.domain.model.OrderSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Página leve: não carrega nem serializa os itens de cada pedido. */
public record OrderListResponse(
        List<OrderListItem> content,
        int page,
        int size,
        boolean hasNext) {

    public record OrderListItem(
            UUID id,
            UUID customerId,
            OrderStatus status,
            BigDecimal totalAmount,
            CurrencyCode currency,
            Instant createdAt,
            Instant updatedAt) {

        static OrderListItem from(OrderSummary order) {
            return new OrderListItem(
                    order.id(),
                    order.customerId(),
                    order.status(),
                    order.totalAmount(),
                    order.currency(),
                    order.createdAt(),
                    order.updatedAt());
        }
    }

    public static OrderListResponse from(OrderSlice slice) {
        return new OrderListResponse(
                slice.content().stream().map(OrderListItem::from).toList(),
                slice.page(),
                slice.size(),
                slice.hasNext());
    }
}
