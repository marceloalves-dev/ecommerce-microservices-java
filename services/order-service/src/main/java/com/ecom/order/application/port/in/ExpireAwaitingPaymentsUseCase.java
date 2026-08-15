package com.ecom.order.application.port.in;

import java.time.Instant;

/** Expira pedidos que passaram do prazo da reserva de estoque. */
public interface ExpireAwaitingPaymentsUseCase {
    int expire(Instant now);
}
