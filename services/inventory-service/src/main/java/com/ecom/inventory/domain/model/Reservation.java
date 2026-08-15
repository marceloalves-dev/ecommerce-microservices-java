package com.ecom.inventory.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Resultado persistido de uma tentativa de reserva; a identidade e o status sao imutaveis. */
public record Reservation(UUID id, UUID orderId, Status status, Instant expiresAt) {
    public enum Status { RESERVED, REJECTED, CONFIRMED, RELEASED }
}
