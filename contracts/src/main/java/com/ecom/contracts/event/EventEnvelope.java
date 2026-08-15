package com.ecom.contracts.event;

import java.time.Instant;
import java.util.UUID;

/** Envelope comum, versionado junto dos contratos de mensageria. */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        String traceId,
        T payload) {

    public static <T> EventEnvelope<T> of(String eventType, UUID aggregateId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), aggregateId, null, payload);
    }
}
