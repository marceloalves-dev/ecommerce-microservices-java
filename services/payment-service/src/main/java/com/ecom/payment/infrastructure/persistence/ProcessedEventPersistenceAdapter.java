package com.ecom.payment.infrastructure.persistence;

import com.ecom.payment.application.port.out.ProcessedEventRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
class ProcessedEventPersistenceAdapter implements ProcessedEventRepository {
    private final EntityManager entityManager;
    ProcessedEventPersistenceAdapter(EntityManager entityManager) { this.entityManager = entityManager; }
    public boolean register(String consumer, UUID eventId) {
        return entityManager.createNativeQuery("INSERT INTO payment_processed_events (consumer_name, event_id) VALUES (:consumer, :eventId) ON CONFLICT DO NOTHING")
                .setParameter("consumer", consumer).setParameter("eventId", eventId).executeUpdate() == 1;
    }
}
