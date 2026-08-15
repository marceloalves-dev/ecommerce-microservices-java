package com.ecom.contracts.event;

import java.util.UUID;

public record PaymentDeclined(UUID orderId, String reason) {
}
