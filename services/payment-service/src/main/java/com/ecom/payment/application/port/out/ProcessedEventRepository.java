package com.ecom.payment.application.port.out;

import java.util.UUID;

public interface ProcessedEventRepository {
    boolean register(String consumerName, UUID eventId);
}
