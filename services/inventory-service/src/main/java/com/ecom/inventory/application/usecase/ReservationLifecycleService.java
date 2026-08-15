package com.ecom.inventory.application.usecase;

import com.ecom.inventory.application.port.out.InventoryRepository;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.UUID;

/** Mantem a compensacao idempotente: confirmar ou liberar duas vezes nao altera o saldo novamente. */
@Singleton
public class ReservationLifecycleService {
    private final InventoryRepository repository;

    public ReservationLifecycleService(InventoryRepository repository) {
        this.repository = repository;
    }

    public void confirm(UUID reservationId) {
        requireId(reservationId);
        repository.confirm(reservationId);
    }

    public void release(UUID reservationId) {
        requireId(reservationId);
        repository.release(reservationId);
    }

    public boolean confirmOnce(String consumerName, UUID eventId, UUID reservationId) {
        requireEvent(consumerName, eventId);
        requireId(reservationId);
        return repository.confirmOnce(consumerName, eventId, reservationId);
    }

    public boolean releaseOnce(String consumerName, UUID eventId, UUID reservationId) {
        requireEvent(consumerName, eventId);
        requireId(reservationId);
        return repository.releaseOnce(consumerName, eventId, reservationId);
    }

    public int releaseExpired(Instant now, int limit) {
        if (now == null) {
            throw new IllegalArgumentException("instante de expiracao obrigatorio");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limite deve ser maior que zero");
        }
        return repository.releaseExpiredReservations(now, limit);
    }

    private static void requireId(UUID reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId obrigatorio");
        }
    }

    private static void requireEvent(String consumerName, UUID eventId) {
        if (consumerName == null || consumerName.isBlank() || eventId == null) {
            throw new IllegalArgumentException("consumerName e eventId obrigatorios");
        }
    }
}
