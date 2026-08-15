package com.ecom.inventory.infrastructure.messaging;

import io.micronaut.configuration.kafka.exceptions.DefaultKafkaListenerExceptionHandler;
import io.micronaut.configuration.kafka.exceptions.KafkaListenerException;
import io.micronaut.configuration.kafka.exceptions.KafkaListenerExceptionHandler;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Encaminha a mensagem para DLT depois das tentativas exponenciais do consumer. */
@Singleton
@Replaces(DefaultKafkaListenerExceptionHandler.class)
class InventoryKafkaExceptionHandler implements KafkaListenerExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaExceptionHandler.class);
    private final InventoryDeadLetterClient deadLetter;

    InventoryKafkaExceptionHandler(InventoryDeadLetterClient deadLetter) {
        this.deadLetter = deadLetter;
    }

    @Override
    public void handle(KafkaListenerException exception) {
        exception.getConsumerRecord().ifPresent(record -> {
            log.error("inventory kafka event exhausted retries; topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), exception);
            deadLetter.publish(String.valueOf(record.value()));
        });
    }
}
