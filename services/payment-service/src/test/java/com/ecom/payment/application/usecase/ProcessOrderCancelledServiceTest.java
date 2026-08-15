package com.ecom.payment.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.payment.application.port.out.PaymentGateway;
import com.ecom.payment.application.port.out.PaymentRepository;
import com.ecom.payment.application.port.out.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderCancelledServiceTest {
    @Test
    void estorna_pagamento_aprovado_quando_o_pedido_e_cancelado() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        UUID orderId = UUID.randomUUID();
        var payment = new PaymentRepository.Payment(UUID.randomUUID(), orderId, BigDecimal.TEN, "BRL",
                PaymentRepository.Payment.Status.APPROVED, null);
        var event = EventEnvelope.of("OrderCancelled", orderId,
                new OrderCancelled(orderId, UUID.randomUUID(), "payment timeout"));
        when(processed.register(any(), any())).thenReturn(true);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(gateway.refund(payment)).thenReturn(new PaymentGateway.Refund(true, null));

        new ProcessOrderCancelledService(payments, processed, gateway).process(event);

        verify(gateway).refund(payment);
        verify(payments).save(new PaymentRepository.Payment(payment.id(), orderId, payment.amount(), payment.currency(),
                PaymentRepository.Payment.Status.REFUNDED, "payment timeout"));
    }
}
