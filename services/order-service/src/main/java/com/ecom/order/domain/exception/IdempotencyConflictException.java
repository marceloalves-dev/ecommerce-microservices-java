package com.ecom.order.domain.exception;

/** A mesma chave de idempotência foi reutilizada para uma requisição diferente. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency-Key ja foi utilizada com outro conteudo");
    }
}
