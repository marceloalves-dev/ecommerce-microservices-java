package com.ecom.order.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, UUID> {
    @Query(value = "SELECT * FROM order_outbox WHERE published_at IS NULL AND dead_lettered_at IS NULL AND next_attempt_at <= now() "
            + "ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<OrderOutboxEntity> lockPending(@Param("limit") int limit);
}
