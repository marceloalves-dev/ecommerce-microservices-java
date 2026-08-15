package com.ecom.payment.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, UUID> {
    @Query(value = "SELECT * FROM payment_outbox WHERE published_at IS NULL AND dead_lettered_at IS NULL AND next_attempt_at <= now() ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<PaymentOutboxEntity> lockPending(@Param("limit") int limit);
}
