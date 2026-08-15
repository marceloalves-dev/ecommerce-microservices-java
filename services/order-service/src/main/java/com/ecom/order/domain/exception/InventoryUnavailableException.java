package com.ecom.order.domain.exception;

/** Estoque esta temporariamente indisponivel; o cliente deve receber 503, nao uma resposta inventada. */
public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
