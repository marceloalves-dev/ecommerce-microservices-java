package com.ecom.order.infrastructure.persistence;

import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Adapter que implementa o port {@link OrderRepository} usando JPA. */
@Component
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.version() == null) {
            entity = OrderPersistenceMapper.toEntity(order);
        } else {
            entity = jpa.findById(order.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "pedido desapareceu durante atualizacao: " + order.id()));
            if (!Objects.equals(entity.getVersion(), order.version())) {
                throw new ObjectOptimisticLockingFailureException(OrderEntity.class, order.id());
            }
            entity.updateFrom(order);
        }
        OrderEntity saved = jpa.saveAndFlush(entity);
        return OrderPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id).map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<Order> findAwaitingPaymentExpiredAt(Instant now, int limit) {
        return jpa.findByStatusAndReservationExpiresAtLessThanEqualOrderByReservationExpiresAtAsc(
                        com.ecom.order.domain.model.OrderStatus.AWAITING_PAYMENT, now, PageRequest.of(0, limit))
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public OrderSlice findSlice(int page, int size) {
        var slice = jpa.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size));
        var content = slice.getContent().stream()
                .map(OrderPersistenceMapper::toSummary)
                .toList();
        return new OrderSlice(content, slice.hasNext());
    }
}
