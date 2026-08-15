package com.ecom.payment.infrastructure.messaging;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
class PaymentMessageRetentionScheduler {
    private final EntityManager entityManager;
    private final int retentionDays;

    PaymentMessageRetentionScheduler(EntityManager entityManager,
                                     @Value("${ecom.messaging.retention-days:30}") int retentionDays) {
        this.entityManager = entityManager;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${ecom.messaging.cleanup-fixed-delay-ms:3600000}")
    @Transactional
    void clean() {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86_400L);
        entityManager.createNativeQuery("DELETE FROM payment_outbox WHERE published_at < :cutoff OR dead_lettered_at < :cutoff")
                .setParameter("cutoff", cutoff).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM payment_processed_events WHERE processed_at < :cutoff")
                .setParameter("cutoff", cutoff).executeUpdate();
    }
}
