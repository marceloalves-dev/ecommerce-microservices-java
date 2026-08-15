package com.ecom.order.infrastructure.messaging;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.PaymentApproved;
import com.ecom.contracts.event.PaymentDeclined;
import com.ecom.order.application.port.in.ProcessPaymentResultUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentResultKafkaListener {
    private final ObjectMapper objectMapper;
    private final ProcessPaymentResultUseCase service;

    @KafkaListener(topics = "payment.approved.v1", groupId = "order-service")
    void approved(String raw) throws Exception {
        service.approved(objectMapper.readValue(raw, new TypeReference<EventEnvelope<PaymentApproved>>() { }));
    }

    @KafkaListener(topics = "payment.declined.v1", groupId = "order-service")
    void declined(String raw) throws Exception {
        service.declined(objectMapper.readValue(raw, new TypeReference<EventEnvelope<PaymentDeclined>>() { }));
    }
}
