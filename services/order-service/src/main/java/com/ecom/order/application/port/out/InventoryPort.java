package com.ecom.order.application.port.out;

import com.ecom.order.domain.model.OrderItem;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

/** Contrato sincrono do estoque, independente de gRPC. */
public interface InventoryPort {
    Reservation reserve(UUID orderId, List<OrderItem> items);

    record Reservation(UUID id, boolean accepted, Instant expiresAt) {
    }
}
