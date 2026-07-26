package com.ecom.order.domain.exception;

/** Lançada quando uma regra de criação ou alteração do pedido é violada. */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
