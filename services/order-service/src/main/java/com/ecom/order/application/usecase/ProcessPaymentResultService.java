package com.ecom.order.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.PaymentApproved;
import com.ecom.contracts.event.PaymentDeclined;
import com.ecom.contracts.event.OrderConfirmed;
import com.ecom.order.application.port.out.OrderEventPublisher;
import com.ecom.order.application.port.in.ProcessPaymentResultUseCase;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.application.port.out.ProcessedEventRepository;
import com.ecom.order.domain.exception.OrderNotFoundException;
import com.ecom.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProcessPaymentResultService implements ProcessPaymentResultUseCase {
    private static final String CONSUMER = "order-payment-result";
    private final OrderRepository orders;
    private final ProcessedEventRepository processedEvents;
    private final OrderEventPublisher events;

    @Transactional
    @Override
    public void approved(EventEnvelope<PaymentApproved> event) {
        if (!processedEvents.register(CONSUMER, event.eventId())) {
            return;
        }
        var order = orders.findById(event.payload().orderId()).orElseThrow(() -> new OrderNotFoundException(event.payload().orderId()));
        if (order.status() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }
        if (order.reservationExpiredAt(java.time.Instant.now())) {
            ExpireAwaitingPaymentsService.cancelAndAppendEvent(order,
                    ExpireAwaitingPaymentsService.PAYMENT_TIMEOUT_REASON, orders, events);
            return;
        }
        order.confirm();
        var saved = orders.save(order);
        events.append("order.confirmed.v1", saved.id(), "OrderConfirmed",
                EventEnvelope.of("OrderConfirmed", saved.id(), new OrderConfirmed(saved.id(), saved.reservationId())));
    }

    @Transactional
    @Override
    public void declined(EventEnvelope<PaymentDeclined> event) {
        if (!processedEvents.register(CONSUMER, event.eventId())) {
            return;
        }
        var order = orders.findById(event.payload().orderId()).orElseThrow(() -> new OrderNotFoundException(event.payload().orderId()));
        if (order.status() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }
        ExpireAwaitingPaymentsService.cancelAndAppendEvent(order, event.payload().reason(), orders, events);
    }
}
