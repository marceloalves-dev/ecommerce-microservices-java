package com.ecom.order.application.port.out;

import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saida para persistencia de pedidos. Implementado na camada de
 * infraestrutura ({@code OrderPersistenceAdapter}). O dominio/aplicacao nao
 * conhece JPA.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    OrderSlice findSlice(int page, int size);

    record OrderSlice(List<OrderSummary> content, boolean hasNext) {
    }
}
