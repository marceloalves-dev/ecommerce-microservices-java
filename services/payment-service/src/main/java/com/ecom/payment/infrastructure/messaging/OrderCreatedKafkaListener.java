package com.ecom.payment.infrastructure.messaging;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCreated;
import com.ecom.payment.application.port.in.ProcessOrderCreatedUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderCreatedKafkaListener {
    private final ObjectMapper objectMapper;
    private final ProcessOrderCreatedUseCase service;
    OrderCreatedKafkaListener(ObjectMapper objectMapper, ProcessOrderCreatedUseCase service) { this.objectMapper = objectMapper; this.service = service; }
    @KafkaListener(topics = "order.created.v1", groupId = "payment-service")
    void receive(String raw) throws Exception {
        service.process(objectMapper.readValue(raw, new TypeReference<EventEnvelope<OrderCreated>>() { }));
    }
}
