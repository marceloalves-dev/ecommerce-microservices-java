package com.ecom.order.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_outbox")
class OrderOutboxEntity {
    @Id private UUID id;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(nullable = false) private String topic;
    @Column(name = "event_type", nullable = false) private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 1000) private String lastError;

    protected OrderOutboxEntity() {
    }

    OrderOutboxEntity(UUID aggregateId, String topic, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.nextAttemptAt = createdAt;
    }

    UUID getAggregateId() { return aggregateId; }
    String getTopic() { return topic; }
    String getPayload() { return payload; }

    void markPublished() { publishedAt = Instant.now(); lastError = null; }
    void scheduleRetry(String error) {
        attempts++;
        nextAttemptAt = Instant.now().plusSeconds(Math.min(60, 1L << Math.min(attempts, 6)));
        lastError = error == null ? "erro desconhecido" : error.substring(0, Math.min(1000, error.length()));
    }
}
