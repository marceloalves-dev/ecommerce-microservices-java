package com.ecom.order.application.port.in;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.PaymentApproved;
import com.ecom.contracts.event.PaymentDeclined;

/** Entrada da aplicacao para eventos vindos do payment-service. */
public interface ProcessPaymentResultUseCase {
    void approved(EventEnvelope<PaymentApproved> event);

    void declined(EventEnvelope<PaymentDeclined> event);
}
