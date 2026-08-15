package com.ecom.inventory.application.port.out;

import com.ecom.inventory.application.port.in.InventoryUseCase;
import com.ecom.inventory.domain.model.Reservation;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

/** Port de persistencia: o caso de uso nao conhece JDBC ou PostgreSQL. */
public interface InventoryRepository {
    InventoryUseCase.Availability check(List<InventoryUseCase.Line> lines);

    Reservation reserve(UUID orderId, List<InventoryUseCase.Line> lines);

    void confirm(UUID reservationId);

    void release(UUID reservationId);

    /**
     * Libera reservas ainda ativas cujo prazo tenha vencido. A implementacao deve
     * processar cada reserva de forma atomica para que execucoes concorrentes nao
     * restituam o mesmo estoque duas vezes.
     */
    int releaseExpiredReservations(Instant now, int limit);
}
