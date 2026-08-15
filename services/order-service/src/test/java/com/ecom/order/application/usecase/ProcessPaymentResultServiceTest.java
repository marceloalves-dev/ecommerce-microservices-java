package com.ecom.order.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.PaymentApproved;
import com.ecom.order.application.port.out.OrderEventPublisher;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.application.port.out.ProcessedEventRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentResultServiceTest {

    @Mock
    OrderRepository orders;

    @Mock
    ProcessedEventRepository processedEvents;

    @Mock
    OrderEventPublisher events;

    @Test
    void aprovacao_apos_expiracao_cancela_pedido_em_vez_de_confirma_lo() {
        Order order = Order.create(UUID.randomUUID(),
                List.of(new OrderItem("SKU-1", 1, BigDecimal.TEN)), CurrencyCode.BRL);
        order.awaitPayment(UUID.randomUUID(), Instant.now().minusSeconds(1));
        EventEnvelope<PaymentApproved> event = EventEnvelope.of("PaymentApproved", order.id(),
                new PaymentApproved(order.id(), UUID.randomUUID(), BigDecimal.TEN, "BRL"));
        when(processedEvents.register("order-payment-result", event.eventId())).thenReturn(true);
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new ProcessPaymentResultService(orders, processedEvents, events).approved(event);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(events).append(eq("order.cancelled.v1"), eq(order.id()), eq("OrderCancelled"), any());
        verify(events, never()).append(eq("order.confirmed.v1"), any(), any(), any());
    }

    @Test
    void evento_repetido_nao_altera_o_pedido_nem_publica_novamente() {
        UUID orderId = UUID.randomUUID();
        EventEnvelope<PaymentApproved> event = EventEnvelope.of("PaymentApproved", orderId,
                new PaymentApproved(orderId, UUID.randomUUID(), BigDecimal.TEN, "BRL"));
        when(processedEvents.register("order-payment-result", event.eventId())).thenReturn(false);

        new ProcessPaymentResultService(orders, processedEvents, events).approved(event);

        verify(orders, never()).findById(any());
        verify(events, never()).append(any(), any(), any(), any());
    }
}
