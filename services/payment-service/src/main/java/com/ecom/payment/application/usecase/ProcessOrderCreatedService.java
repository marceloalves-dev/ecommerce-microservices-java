package com.ecom.payment.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCreated;
import com.ecom.contracts.event.PaymentApproved;
import com.ecom.contracts.event.PaymentDeclined;
import com.ecom.payment.application.port.in.ProcessOrderCreatedUseCase;
import com.ecom.payment.application.port.out.PaymentEventPublisher;
import com.ecom.payment.application.port.out.PaymentGateway;
import com.ecom.payment.application.port.out.PaymentRepository;
import com.ecom.payment.application.port.out.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ProcessOrderCreatedService implements ProcessOrderCreatedUseCase {
    private static final String CONSUMER = "payment-order-created";
    private final PaymentRepository payments;
    private final ProcessedEventRepository processedEvents;
    private final PaymentGateway gateway;
    private final PaymentEventPublisher events;

    ProcessOrderCreatedService(PaymentRepository payments, ProcessedEventRepository processedEvents,
                               PaymentGateway gateway, PaymentEventPublisher events) {
        this.payments = payments;
        this.processedEvents = processedEvents;
        this.gateway = gateway;
        this.events = events;
    }

    @Override
    @Transactional
    public void process(EventEnvelope<OrderCreated> event) {
        if (!processedEvents.register(CONSUMER, event.eventId()) || payments.findByOrderId(event.payload().orderId()).isPresent()) {
            return;
        }
        PaymentGateway.Authorization authorization = gateway.authorize(event.payload());
        var payment = payments.save(new PaymentRepository.Payment(UUID.randomUUID(), event.payload().orderId(),
                event.payload().totalAmount(), event.payload().currency(),
                authorization.approved() ? PaymentRepository.Payment.Status.APPROVED : PaymentRepository.Payment.Status.DECLINED,
                authorization.reason()));
        if (payment.status() == PaymentRepository.Payment.Status.APPROVED) {
            events.append("payment.approved.v1", payment.orderId(), "PaymentApproved",
                    EventEnvelope.of("PaymentApproved", payment.orderId(),
                            new PaymentApproved(payment.orderId(), payment.id(), payment.amount(), payment.currency())));
        } else {
            events.append("payment.declined.v1", payment.orderId(), "PaymentDeclined",
                    EventEnvelope.of("PaymentDeclined", payment.orderId(),
                            new PaymentDeclined(payment.orderId(), payment.reason())));
        }
    }
}
