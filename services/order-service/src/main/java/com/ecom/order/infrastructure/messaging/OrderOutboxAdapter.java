package com.ecom.order.infrastructure.messaging;

import com.ecom.order.application.port.out.OrderEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderOutboxAdapter implements OrderEventPublisher {
    private final OrderOutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void append(String topic, UUID aggregateId, String eventType, Object payload) {
        try {
            repository.save(new OrderOutboxEntity(aggregateId, topic, eventType,
                    objectMapper.writeValueAsString(payload)));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("evento nao pode ser serializado", ex);
        }
    }
}
