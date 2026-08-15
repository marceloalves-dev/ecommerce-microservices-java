package com.ecom.order.infrastructure.messaging;

import com.ecom.order.application.port.out.ProcessedEventRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProcessedEventPersistenceAdapter implements ProcessedEventRepository {
    private final EntityManager entityManager;

    @Override
    public boolean register(String consumerName, UUID eventId) {
        int inserted = entityManager.createNativeQuery("INSERT INTO order_processed_events (consumer_name, event_id) "
                        + "VALUES (:consumer, :eventId) ON CONFLICT DO NOTHING")
                .setParameter("consumer", consumerName).setParameter("eventId", eventId).executeUpdate();
        return inserted == 1;
    }
}
