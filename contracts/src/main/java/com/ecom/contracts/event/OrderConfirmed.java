package com.ecom.contracts.event;

import java.util.UUID;

public record OrderConfirmed(UUID orderId, UUID reservationId) {
}
