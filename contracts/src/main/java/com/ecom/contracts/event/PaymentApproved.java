package com.ecom.contracts.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentApproved(UUID orderId, UUID paymentId, BigDecimal amount, String currency) {
}
