package com.ecom.order.application.usecase;

import com.ecom.order.application.port.out.OrderEventPublisher;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.domain.model.CurrencyCode;
import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderItem;
import com.ecom.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpireAwaitingPaymentsServiceTest {

    @Mock
    OrderRepository orders;

    @Mock
    OrderEventPublisher events;

    @Test
    void cancela_pedido_vencido_e_publica_compensacao() {
        Instant now = Instant.parse("2026-08-15T16:00:00Z");
        Order order = awaitingPayment(now.minusSeconds(1));
        when(orders.findAwaitingPaymentExpiredAt(now, 100)).thenReturn(List.of(order));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expired = new ExpireAwaitingPaymentsService(orders, events).expire(now);

        assertThat(expired).isEqualTo(1);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellationReason()).isEqualTo("payment timeout");
        verify(events).append(eq("order.cancelled.v1"), eq(order.id()), eq("OrderCancelled"), any());
    }

    private static Order awaitingPayment(Instant expiresAt) {
        Order order = Order.create(UUID.randomUUID(),
                List.of(new OrderItem("SKU-1", 1, BigDecimal.TEN)), CurrencyCode.BRL);
        order.awaitPayment(UUID.randomUUID(), expiresAt);
        return order;
    }
}
