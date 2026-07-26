package com.ecom.order.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO order_idempotency
                (id, customer_id, idempotency_key, request_hash, created_at)
            VALUES (:id, :customerId, :idempotencyKey, :requestHash, :createdAt)
            ON CONFLICT (customer_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("customerId") UUID customerId,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("requestHash") String requestHash,
                       @Param("createdAt") Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
              from IdempotencyEntity request
             where request.customerId = :customerId
               and request.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyEntity> findForUpdate(@Param("customerId") UUID customerId,
                                              @Param("idempotencyKey") String idempotencyKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update IdempotencyEntity request
               set request.orderId = :orderId
             where request.id = :claimId
               and request.orderId is null
            """)
    int complete(@Param("claimId") UUID claimId, @Param("orderId") UUID orderId);
}
