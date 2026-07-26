package com.ecom.order.application.port.in;

import com.ecom.order.domain.model.Order;

import java.util.UUID;

/** Casos de uso de leitura de pedidos. */
public interface GetOrderUseCase {

    /** @throws com.ecom.order.domain.exception.OrderNotFoundException se nao existir */
    Order getById(UUID id);

    OrderSlice list(int page, int size);

    record OrderSlice(
            java.util.List<com.ecom.order.domain.model.OrderSummary> content,
            int page,
            int size,
            boolean hasNext) {
    }
}
