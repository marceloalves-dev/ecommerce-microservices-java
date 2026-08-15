package com.ecom.payment.infrastructure.messaging;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCreated;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.payment.application.port.in.ProcessOrderCancelledUseCase;
import com.ecom.payment.application.port.in.ProcessOrderCreatedUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderCreatedKafkaListener {
    private final ObjectMapper objectMapper;
    private final ProcessOrderCreatedUseCase service;
    private final ProcessOrderCancelledUseCase cancellations;
    OrderCreatedKafkaListener(ObjectMapper objectMapper, ProcessOrderCreatedUseCase service,
                              ProcessOrderCancelledUseCase cancellations) {
        this.objectMapper = objectMapper;
        this.service = service;
        this.cancellations = cancellations;
    }
    @KafkaListener(topics = "order.created.v1", groupId = "payment-service")
    void receive(String raw) throws Exception {
        service.process(objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderCreated>>() { }));
    }

    @KafkaListener(topics = "order.cancelled.v1", groupId = "payment-service")
    void cancelled(String raw) throws Exception {
        cancellations.process(objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderCancelled>>() { }));
    }
}
