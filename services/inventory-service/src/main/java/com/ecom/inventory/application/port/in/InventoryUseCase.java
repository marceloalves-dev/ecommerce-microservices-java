package com.ecom.inventory.application.port.in;

import com.ecom.inventory.domain.model.Reservation;

import java.util.List;
import java.util.UUID;

public interface InventoryUseCase {
    Availability check(List<Line> lines);

    Reservation reserve(UUID orderId, List<Line> lines);

    record Line(String sku, int quantity) {
    }

    record Availability(boolean available, List<String> unavailableSkus) {
    }
}
