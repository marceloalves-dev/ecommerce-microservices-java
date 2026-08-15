package com.ecom.payment.application.port.out;

import java.util.UUID;

public interface PaymentEventPublisher {
    void append(String topic, UUID aggregateId, String eventType, Object payload);
}
