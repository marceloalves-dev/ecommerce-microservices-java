package com.ecom.order.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/** Publicacao at-least-once: a marcacao so ocorre depois do ACK do broker. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ecom.outbox.enabled", havingValue = "true", matchIfMissing = true)
class OrderOutboxPublisher {
    private static final int BATCH_SIZE = 25;
    private final OrderOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelayString = "${ecom.outbox.fixed-delay-ms:500}")
    @Transactional
    public void publishPending() {
        for (OrderOutboxEntity event : repository.lockPending(BATCH_SIZE)) {
            try {
                kafka.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception ex) {
                event.scheduleRetry(ex.getMessage());
            }
        }
    }
}
