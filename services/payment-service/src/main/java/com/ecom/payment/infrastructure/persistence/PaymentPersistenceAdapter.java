package com.ecom.payment.infrastructure.persistence;

import com.ecom.payment.application.port.out.PaymentRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
class PaymentPersistenceAdapter implements PaymentRepository {
    private final PaymentJpaRepository repository;
    PaymentPersistenceAdapter(PaymentJpaRepository repository) { this.repository = repository; }
    public Optional<Payment> findByOrderId(UUID orderId) { return repository.findByOrderId(orderId).map(PaymentEntity::toDomain); }
    public Payment save(Payment payment) { return repository.save(new PaymentEntity(payment)).toDomain(); }
}
