package com.ecom.payment.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCreated;
import com.ecom.payment.application.port.out.PaymentEventPublisher;
import com.ecom.payment.application.port.out.PaymentGateway;
import com.ecom.payment.application.port.out.PaymentRepository;
import com.ecom.payment.application.port.out.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderCreatedServiceTest {
    @Test
    void evento_ja_processado_nao_cobra_nem_publica_novamente() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentEventPublisher events = mock(PaymentEventPublisher.class);
        when(processed.register(any(), any())).thenReturn(false);

        var service = new ProcessOrderCreatedService(payments, processed, gateway, events);
        UUID orderId = UUID.randomUUID();
        service.process(EventEnvelope.of("OrderCreated", orderId,
                new OrderCreated(orderId, UUID.randomUUID(), List.of(), BigDecimal.TEN, "BRL", UUID.randomUUID().toString())));

        verify(payments, never()).findByOrderId(any());
        verify(gateway, never()).authorize(any());
        verify(events, never()).append(any(), any(), any(), any());
    }

    @Test
    void aprovacao_persiste_pagamento_e_emite_evento() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentEventPublisher events = mock(PaymentEventPublisher.class);
        UUID orderId = UUID.randomUUID();
        when(processed.register(any(), any())).thenReturn(true);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(gateway.authorize(any())).thenReturn(new PaymentGateway.Authorization(true, null));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new ProcessOrderCreatedService(payments, processed, gateway, events);
        service.process(EventEnvelope.of("OrderCreated", orderId,
                new OrderCreated(orderId, UUID.randomUUID(), List.of(), BigDecimal.TEN, "BRL", UUID.randomUUID().toString())));

        verify(events).append(org.mockito.ArgumentMatchers.eq("payment.approved.v1"),
                org.mockito.ArgumentMatchers.eq(orderId), org.mockito.ArgumentMatchers.eq("PaymentApproved"), any());
    }
}
