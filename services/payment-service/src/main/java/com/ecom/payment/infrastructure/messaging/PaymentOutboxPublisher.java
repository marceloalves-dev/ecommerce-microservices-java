package com.ecom.payment.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "ecom.outbox.enabled", havingValue = "true", matchIfMissing = true)
class PaymentOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
    private final PaymentOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final int maxAttempts;
    PaymentOutboxPublisher(PaymentOutboxJpaRepository repository, KafkaTemplate<String, String> kafka,
                           @Value("${ecom.outbox.max-attempts:10}") int maxAttempts) {
        this.repository = repository;
        this.kafka = kafka;
        this.maxAttempts = maxAttempts;
    }
    @Scheduled(fixedDelayString = "${ecom.outbox.fixed-delay-ms:500}")
    @Transactional
    void publish() {
        for (PaymentOutboxEntity event : repository.lockPending(25)) {
            try { kafka.send(event.topic(), event.aggregateId().toString(), event.payload()).get(5, TimeUnit.SECONDS); event.published(); }
            catch (Exception ex) {
                if (event.retry(ex.getMessage(), maxAttempts)) {
                    log.error("outbox event exhausted retries; topic={}, aggregateId={}", event.topic(), event.aggregateId(), ex);
                }
            }
        }
    }
}
