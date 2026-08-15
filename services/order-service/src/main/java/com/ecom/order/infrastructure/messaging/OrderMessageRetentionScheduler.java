package com.ecom.order.infrastructure.messaging;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Mantem outbox e inbox finitos sem remover mensagens ainda pendentes. */
@Component
class OrderMessageRetentionScheduler {
    private final EntityManager entityManager;
    private final int retentionDays;

    OrderMessageRetentionScheduler(EntityManager entityManager,
                                   @Value("${ecom.messaging.retention-days:30}") int retentionDays) {
        this.entityManager = entityManager;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${ecom.messaging.cleanup-fixed-delay-ms:3600000}")
    @Transactional
    void clean() {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86_400L);
        entityManager.createNativeQuery("DELETE FROM order_outbox WHERE published_at < :cutoff OR dead_lettered_at < :cutoff")
                .setParameter("cutoff", cutoff).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM order_processed_events WHERE processed_at < :cutoff")
                .setParameter("cutoff", cutoff).executeUpdate();
    }
}
