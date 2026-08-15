package com.ecom.payment.application.port.in;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;

/** Solicita estorno quando a saga cancela um pedido ja cobrado. */
public interface ProcessOrderCancelledUseCase {
    void process(EventEnvelope<OrderCancelled> event);
}
