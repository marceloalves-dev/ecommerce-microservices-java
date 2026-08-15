package com.ecom.inventory.infrastructure.messaging;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.contracts.event.OrderConfirmed;
import com.ecom.inventory.application.usecase.ReservationLifecycleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaListener(groupId = "inventory-service")
public class OrderLifecycleKafkaListener {
    private final ObjectMapper objectMapper;
    private final ReservationLifecycleService reservations;

    public OrderLifecycleKafkaListener(ObjectMapper objectMapper, ReservationLifecycleService reservations) {
        this.objectMapper = objectMapper;
        this.reservations = reservations;
    }

    @Topic("order.confirmed.v1")
    public void confirmed(String raw) throws Exception {
        var event = objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderConfirmed>>() { });
        reservations.confirm(event.payload().reservationId());
    }

    @Topic("order.cancelled.v1")
    public void cancelled(String raw) throws Exception {
        var event = objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderCancelled>>() { });
        reservations.release(event.payload().reservationId());
    }
}
