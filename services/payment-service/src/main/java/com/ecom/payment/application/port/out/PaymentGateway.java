package com.ecom.payment.application.port.out;

import com.ecom.contracts.event.OrderCreated;

/** Port do adquirente; a Fase 2 usa um adapter mock deterministico. */
public interface PaymentGateway {
    Authorization authorize(OrderCreated order);

    Refund refund(PaymentRepository.Payment payment);

    record Authorization(boolean approved, String reason) {
    }

    record Refund(boolean successful, String reason) {
    }
}
