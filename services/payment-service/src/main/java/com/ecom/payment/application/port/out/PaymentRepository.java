package com.ecom.payment.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Optional<Payment> findByOrderId(UUID orderId);

    Payment save(Payment payment);

    record Payment(UUID id, UUID orderId, BigDecimal amount, String currency, Status status, String reason) {
        public enum Status { APPROVED, DECLINED, REFUNDED }
    }
}
