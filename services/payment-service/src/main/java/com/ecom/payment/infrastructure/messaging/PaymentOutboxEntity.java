package com.ecom.payment.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox")
class PaymentOutboxEntity {
    @Id private UUID id;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(nullable = false) private String topic;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 1000) private String lastError;
    protected PaymentOutboxEntity() { }
    PaymentOutboxEntity(UUID aggregateId, String topic, String payload) {
        id = UUID.randomUUID(); this.aggregateId = aggregateId; this.topic = topic; this.payload = payload;
        createdAt = Instant.now(); nextAttemptAt = createdAt;
    }
    UUID aggregateId() { return aggregateId; } String topic() { return topic; } String payload() { return payload; }
    void published() { publishedAt = Instant.now(); lastError = null; }
    void retry(String error) { attempts++; nextAttemptAt = Instant.now().plusSeconds(Math.min(60, 1L << Math.min(attempts, 6))); lastError = error == null ? "erro desconhecido" : error.substring(0, Math.min(1000, error.length())); }
}
