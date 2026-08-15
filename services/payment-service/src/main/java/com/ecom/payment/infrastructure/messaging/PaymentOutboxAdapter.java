package com.ecom.payment.infrastructure.messaging;

import com.ecom.payment.application.port.out.PaymentEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
class PaymentOutboxAdapter implements PaymentEventPublisher {
    private final PaymentOutboxJpaRepository repository;
    private final ObjectMapper objectMapper;
    PaymentOutboxAdapter(PaymentOutboxJpaRepository repository, ObjectMapper objectMapper) { this.repository = repository; this.objectMapper = objectMapper; }
    public void append(String topic, UUID aggregateId, String eventType, Object payload) {
        try { repository.save(new PaymentOutboxEntity(aggregateId, topic, objectMapper.writeValueAsString(payload))); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("evento nao pode ser serializado", ex); }
    }
}
