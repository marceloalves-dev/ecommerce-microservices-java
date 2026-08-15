package com.ecom.payment.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "ecom.outbox.enabled", havingValue = "true", matchIfMissing = true)
class PaymentOutboxPublisher {
    private final PaymentOutboxJpaRepository repository;
    private final KafkaTemplate<String, String> kafka;
    PaymentOutboxPublisher(PaymentOutboxJpaRepository repository, KafkaTemplate<String, String> kafka) { this.repository = repository; this.kafka = kafka; }
    @Scheduled(fixedDelayString = "${ecom.outbox.fixed-delay-ms:500}")
    @Transactional
    void publish() {
        for (PaymentOutboxEntity event : repository.lockPending(25)) {
            try { kafka.send(event.topic(), event.aggregateId().toString(), event.payload()).get(5, TimeUnit.SECONDS); event.published(); }
            catch (Exception ex) { event.retry(ex.getMessage()); }
        }
    }
}
