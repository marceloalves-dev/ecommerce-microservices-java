package com.ecom.payment.infrastructure.messaging;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class KafkaConsumerErrorConfiguration {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerErrorConfiguration.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka, (record, error) -> {
            log.error("kafka event exhausted retries; topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), error);
            return new TopicPartition(record.topic() + ".DLT", record.partition());
        });
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
