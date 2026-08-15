package com.ecom.payment.application.usecase;

import com.ecom.contracts.event.EventEnvelope;
import com.ecom.contracts.event.OrderCancelled;
import com.ecom.payment.application.port.in.ProcessOrderCancelledUseCase;
import com.ecom.payment.application.port.out.PaymentGateway;
import com.ecom.payment.application.port.out.PaymentRepository;
import com.ecom.payment.application.port.out.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProcessOrderCancelledService implements ProcessOrderCancelledUseCase {
    private static final String CONSUMER = "payment-order-cancelled";
    private final PaymentRepository payments;
    private final ProcessedEventRepository processedEvents;
    private final PaymentGateway gateway;

    ProcessOrderCancelledService(PaymentRepository payments, ProcessedEventRepository processedEvents, PaymentGateway gateway) {
        this.payments = payments;
        this.processedEvents = processedEvents;
        this.gateway = gateway;
    }

    @Override
    @Transactional
    public void process(EventEnvelope<OrderCancelled> event) {
        if (!processedEvents.register(CONSUMER, event.eventId())) {
            return;
        }
        var payment = payments.findByOrderId(event.payload().orderId()).orElse(null);
        if (payment == null || payment.status() != PaymentRepository.Payment.Status.APPROVED) {
            return;
        }
        PaymentGateway.Refund refund = gateway.refund(payment);
        if (!refund.successful()) {
            throw new IllegalStateException("estorno recusado pelo gateway: " + refund.reason());
        }
        payments.save(new PaymentRepository.Payment(payment.id(), payment.orderId(), payment.amount(), payment.currency(),
                PaymentRepository.Payment.Status.REFUNDED, event.payload().reason()));
    }
}
