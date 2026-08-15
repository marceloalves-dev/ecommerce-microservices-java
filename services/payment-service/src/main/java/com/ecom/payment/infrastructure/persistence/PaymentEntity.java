package com.ecom.payment.infrastructure.persistence;

import com.ecom.payment.application.port.out.PaymentRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
class PaymentEntity {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false, unique = true) private UUID orderId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PaymentRepository.Payment.Status status;
    @Column(length = 500) private String reason;
    protected PaymentEntity() { }
    PaymentEntity(PaymentRepository.Payment p) { id = p.id(); orderId = p.orderId(); amount = p.amount(); currency = p.currency(); status = p.status(); reason = p.reason(); }
    PaymentRepository.Payment toDomain() { return new PaymentRepository.Payment(id, orderId, amount, currency, status, reason); }
}
