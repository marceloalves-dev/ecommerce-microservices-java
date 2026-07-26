package com.ecom.order.domain.exception;

import com.ecom.order.domain.model.OrderStatus;

/**
 * Lancada quando se tenta uma transicao de estado invalida na saga do pedido.
 * Ex.: confirmar um pedido que ainda esta PENDING.
 */
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Transicao invalida: %s -> %s".formatted(from, to));
    }
}
