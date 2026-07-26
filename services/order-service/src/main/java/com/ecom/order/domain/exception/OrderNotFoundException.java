package com.ecom.order.domain.exception;

import java.util.UUID;

/** Lancada quando um pedido nao existe. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Pedido nao encontrado: " + orderId);
    }
}
