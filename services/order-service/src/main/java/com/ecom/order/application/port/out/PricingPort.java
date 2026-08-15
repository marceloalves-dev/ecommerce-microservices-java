package com.ecom.order.application.port.out;

import com.ecom.order.domain.model.CurrencyCode;

import java.math.BigDecimal;

/** Fonte confiável de preço; nesta fase é um catálogo local substituível. */
public interface PricingPort {

    Price getPrice(String sku);

    record Price(BigDecimal amount, CurrencyCode currency) {
    }
}
