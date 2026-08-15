package com.ecom.order.application.port.out;

import java.util.UUID;

/**
 * Serializa criações com a mesma chave dentro da transação do pedido.
 * Uma claim concluída aponta para o pedido originalmente criado.
 */
public interface IdempotencyRepository {

    Claim claim(UUID customerId, String idempotencyKey, String requestHash);

    void complete(UUID claimId, UUID orderId);

    record Claim(UUID id, String requestHash, UUID orderId) {
    }
}
