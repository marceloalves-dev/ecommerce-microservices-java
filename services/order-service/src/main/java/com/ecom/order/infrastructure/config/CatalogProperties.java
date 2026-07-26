package com.ecom.order.infrastructure.config;

import com.ecom.order.domain.model.CurrencyCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "ecom.catalog")
@Validated
public record CatalogProperties(
        @NotNull CurrencyCode currency,
        @NotEmpty Map<String, @NotNull BigDecimal> prices) {
}
