package com.ecom.order.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findByStatusAndReservationExpiresAtLessThanEqualOrderByReservationExpiresAtAsc(
            com.ecom.order.domain.model.OrderStatus status, Instant now, Pageable pageable);

    Slice<OrderEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
