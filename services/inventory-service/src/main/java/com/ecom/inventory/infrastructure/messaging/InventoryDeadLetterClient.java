package com.ecom.inventory.infrastructure.messaging;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
interface InventoryDeadLetterClient {
    @Topic("inventory.order-lifecycle.dlt")
    void publish(String payload);
}
