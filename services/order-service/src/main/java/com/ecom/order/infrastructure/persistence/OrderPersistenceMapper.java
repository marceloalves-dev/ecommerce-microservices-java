package com.ecom.order.infrastructure.persistence;

import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderItem;
import com.ecom.order.domain.model.OrderSummary;

import java.util.List;

/** Traducao entre o agregado de dominio e a entidade JPA. */
final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity(
                order.id(),
                order.customerId(),
                order.status(),
                order.totalAmount(),
                order.currency(),
                order.reservationId(),
                order.cancellationReason(),
                order.createdAt(),
                order.updatedAt(),
                order.version());
        for (OrderItem item : order.items()) {
            entity.addItem(new OrderItemEntity(item.sku(), item.quantity(), item.unitPrice()));
        }
        return entity;
    }

    static Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getSku(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return Order.reconstitute(
                entity.getId(),
                entity.getCustomerId(),
                items,
                entity.getTotalAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getReservationId(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    static OrderSummary toSummary(OrderEntity entity) {
        return new OrderSummary(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
