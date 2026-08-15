package com.ecom.inventory;

import io.micronaut.runtime.Micronaut;

public final class InventoryApplication {
    private InventoryApplication() {
    }

    public static void main(String[] args) {
        Micronaut.run(InventoryApplication.class, args);
    }
}
