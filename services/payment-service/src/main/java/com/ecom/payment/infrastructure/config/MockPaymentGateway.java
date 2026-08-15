package com.ecom.payment.infrastructure.config;

import com.ecom.contracts.event.OrderCreated;
import com.ecom.payment.application.port.out.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Simulador explicito: em dev aprova por padrao, sem usar dados enviados pelo cliente. */
@Component
class MockPaymentGateway implements PaymentGateway {
    private final boolean approve;

    MockPaymentGateway(@Value("${ecom.payment.mock.approve:true}") boolean approve) {
        this.approve = approve;
    }

    @Override
    public Authorization authorize(OrderCreated order) {
        return approve ? new Authorization(true, null) : new Authorization(false, "pagamento recusado pelo gateway mock");
    }
}
