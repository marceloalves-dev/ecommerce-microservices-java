package com.ecom.contracts.event;

import java.util.UUID;

public record OrderCancelled(UUID orderId, UUID reservationId, String reason) {
}
