package com.ecom.order.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.order.application.port.in.ExpireAwaitingPaymentsUseCase;
import com.ecom.order.application.port.out.OrderEventPublisher;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class ExpireAwaitingPaymentsService implements ExpireAwaitingPaymentsUseCase {
    private static final int BATCH_SIZE = 100;
    static final String PAYMENT_TIMEOUT_REASON = "payment timeout";

    private final OrderRepository orders;
    private final OrderEventPublisher events;

    @Override
    @Transactional
    public int expire(Instant now) {
        int expired = 0;
        for (Order order : orders.findAwaitingPaymentExpiredAt(now, BATCH_SIZE)) {
            if (!order.reservationExpiredAt(now)) {
                continue;
            }
            cancelAndAppendEvent(order, PAYMENT_TIMEOUT_REASON);
            expired++;
        }
        return expired;
    }

    static void cancelAndAppendEvent(Order order, String reason, OrderRepository orders, OrderEventPublisher events) {
        order.cancel(reason);
        Order saved = orders.save(order);
        events.append("order.cancelled.v1", saved.id(), "OrderCancelled",
                EventEnvelope.of("OrderCancelled", saved.id(),
                        new OrderCancelled(saved.id(), saved.reservationId(), saved.cancellationReason())));
    }

    private void cancelAndAppendEvent(Order order, String reason) {
        cancelAndAppendEvent(order, reason, orders, events);
    }
}
