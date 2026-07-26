package com.ecom.order.domain.model;

import com.ecom.order.domain.exception.InvalidOrderException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item de pedido — Value Object imutavel.
 *
 * @param sku       identificador do produto
 * @param quantity  quantidade (> 0)
 * @param unitPrice preco unitario (>= 0)
 */
public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {

    public static final int MAX_SKU_LENGTH = 100;
    public static final int MAX_QUANTITY = 10_000;

    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new InvalidOrderException("sku obrigatorio");
        }
        sku = sku.trim();
        if (sku.length() > MAX_SKU_LENGTH) {
            throw new InvalidOrderException("sku deve ter no maximo 100 caracteres");
        }
        if (quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new InvalidOrderException("quantity deve estar entre 1 e 10000");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidOrderException("unitPrice deve ser >= 0");
        }
        try {
            unitPrice = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidOrderException("unitPrice deve ter no maximo 2 casas decimais");
        }
        if (unitPrice.precision() > 19) {
            throw new InvalidOrderException("unitPrice excede o limite monetario");
        }
    }

    /** Total da linha = unitPrice * quantity. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
