package com.ecom.order.application.port.out;

import java.util.UUID;

/** Registra um evento para publicacao assincrona; a implementacao usa a outbox local. */
public interface OrderEventPublisher {
    void append(String topic, UUID aggregateId, String eventType, Object payload);
}
