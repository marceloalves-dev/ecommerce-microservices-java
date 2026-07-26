package com.ecom.order.infrastructure.persistence;

import com.ecom.order.application.port.out.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class IdempotencyPersistenceAdapter implements IdempotencyRepository {

    private final IdempotencyJpaRepository jpa;

    @Override
    public Claim claim(UUID customerId, String idempotencyKey, String requestHash) {
        jpa.insertIfAbsent(
                UUID.randomUUID(), customerId, idempotencyKey, requestHash, Instant.now());
        IdempotencyEntity entity = jpa.findForUpdate(customerId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "falha ao adquirir registro de idempotencia"));
        return new Claim(entity.getId(), entity.getRequestHash(), entity.getOrderId());
    }

    @Override
    public void complete(UUID claimId, UUID orderId) {
        if (jpa.complete(claimId, orderId) != 1) {
            throw new IllegalStateException("claim de idempotencia ja concluida");
        }
    }
}
