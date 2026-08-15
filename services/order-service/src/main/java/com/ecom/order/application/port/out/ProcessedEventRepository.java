package com.ecom.order.application.port.out;

import java.util.UUID;

/** Deduplicacao atomica de eventos recebidos, na mesma transacao da alteracao do pedido. */
public interface ProcessedEventRepository {
    boolean register(String consumerName, UUID eventId);
}
