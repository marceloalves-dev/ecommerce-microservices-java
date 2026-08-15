package com.ecom.inventory.infrastructure.messaging;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.contracts.event.OrderConfirmed;
import com.ecom.inventory.application.usecase.ReservationLifecycleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.configuration.kafka.annotation.ErrorStrategy;
import io.micronaut.configuration.kafka.annotation.ErrorStrategyValue;

@KafkaListener(groupId = "inventory-service")
@ErrorStrategy(value = ErrorStrategyValue.RETRY_EXPONENTIALLY_ON_ERROR, retryCount = 3, retryDelay = "1s")
public class OrderLifecycleKafkaListener {
    private static final String CONSUMER = "inventory-order-lifecycle";
    private final ObjectMapper objectMapper;
    private final ReservationLifecycleService reservations;

    public OrderLifecycleKafkaListener(ObjectMapper objectMapper, ReservationLifecycleService reservations) {
        this.objectMapper = objectMapper;
        this.reservations = reservations;
    }

    @Topic("order.confirmed.v1")
    public void confirmed(String raw) throws Exception {
        var event = objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderConfirmed>>() { });
        reservations.confirmOnce(CONSUMER, event.eventId(), event.payload().reservationId());
    }

    @Topic("order.cancelled.v1")
    public void cancelled(String raw) throws Exception {
        var event = objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderCancelled>>() { });
        reservations.releaseOnce(CONSUMER, event.eventId(), event.payload().reservationId());
    }
}
