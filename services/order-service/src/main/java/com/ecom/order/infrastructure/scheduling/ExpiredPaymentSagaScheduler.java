package com.ecom.order.infrastructure.scheduling;

import com.ecom.order.application.port.in.ExpireAwaitingPaymentsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Fecha a saga quando o pagamento nao termina dentro do prazo da reserva. */
@Component
public class ExpiredPaymentSagaScheduler {
    private final ExpireAwaitingPaymentsUseCase expiration;

    public ExpiredPaymentSagaScheduler(ExpireAwaitingPaymentsUseCase expiration) {
        this.expiration = expiration;
    }

    @Scheduled(fixedDelayString = "${ecom.saga.payment-expiration.fixed-delay-ms:60000}")
    void expireAwaitingPayments() {
        expiration.expire(Instant.now());
    }
}
