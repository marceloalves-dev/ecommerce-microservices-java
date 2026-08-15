package com.ecom.payment.application.port.in;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCreated;

public interface ProcessOrderCreatedUseCase {
    void process(EventEnvelope<OrderCreated> event);
}
