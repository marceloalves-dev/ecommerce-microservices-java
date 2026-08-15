package com.ecom.inventory.application.usecase;

import com.ecom.inventory.application.port.in.InventoryUseCase;
import com.ecom.inventory.application.port.out.InventoryRepository;
import com.ecom.inventory.domain.model.Reservation;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Singleton
public class InventoryService implements InventoryUseCase {
    private static final int MAX_LINES = 100;
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Availability check(List<Line> lines) {
        validate(lines);
        return repository.check(lines);
    }

    @Override
    public Reservation reserve(UUID orderId, List<Line> lines) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId obrigatorio");
        }
        validate(lines);
        return repository.reserve(orderId, lines);
    }

    private static void validate(List<Line> lines) {
        if (lines == null || lines.isEmpty() || lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("pedido deve conter entre 1 e 100 itens");
        }
        var skus = new HashSet<String>();
        for (Line line : lines) {
            if (line == null || line.sku() == null || line.sku().isBlank()
                    || line.sku().trim().length() > 100 || line.quantity() <= 0 || line.quantity() > 10_000) {
                throw new IllegalArgumentException("item de estoque invalido");
            }
            if (!skus.add(line.sku().trim())) {
                throw new IllegalArgumentException("sku duplicado: " + line.sku());
            }
        }
    }
}
