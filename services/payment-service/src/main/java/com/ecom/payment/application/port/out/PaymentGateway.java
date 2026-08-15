package com.ecom.payment.application.port.out;

import com.ecom.contracts.event.OrderCreated;

/** Port do adquirente; a Fase 2 usa um adapter mock deterministico. */
public interface PaymentGateway {
    Authorization authorize(OrderCreated order);

    record Authorization(boolean approved, String reason) {
    }
}
