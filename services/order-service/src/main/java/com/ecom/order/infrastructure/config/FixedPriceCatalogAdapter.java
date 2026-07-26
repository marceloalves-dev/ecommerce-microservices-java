package com.ecom.order.infrastructure.config;

import com.ecom.order.application.port.out.PricingPort;
import com.ecom.order.domain.exception.InvalidOrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FixedPriceCatalogAdapter implements PricingPort {

    private final CatalogProperties properties;

    @Override
    public Price getPrice(String sku) {
        if (sku == null) {
            throw new InvalidOrderException("sku obrigatorio");
        }
        var amount = properties.prices().get(sku.trim());
        if (amount == null) {
            throw new InvalidOrderException("sku desconhecido: " + sku);
        }
        return new Price(amount, properties.currency());
    }
}
