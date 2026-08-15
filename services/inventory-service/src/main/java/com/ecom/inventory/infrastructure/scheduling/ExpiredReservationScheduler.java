package com.ecom.inventory.infrastructure.scheduling;

import com.ecom.inventory.application.usecase.ReservationLifecycleService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.time.Instant;

/** Libera estoque abandonado quando o fluxo de pagamento nao chega ao fim. */
@Singleton
public class ExpiredReservationScheduler {
    private static final int BATCH_SIZE = 100;

    private final ReservationLifecycleService reservations;

    public ExpiredReservationScheduler(ReservationLifecycleService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelay = "${inventory.reservation-expiration.fixed-delay:1m}")
    void releaseExpiredReservations() {
        reservations.releaseExpired(Instant.now(), BATCH_SIZE);
    }
}
