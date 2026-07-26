package com.ecom.order.application.port.in;

import com.ecom.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

/** Caso de uso: criar um pedido. */
public interface CreateOrderUseCase {

    Order create(CreateOrderCommand command);

    /** Comando de entrada — desacopla o dominio dos DTOs da API. */
    record CreateOrderCommand(UUID customerId, String idempotencyKey, List<Line> items) {

        public record Line(String sku, int quantity) {
        }
    }
}
