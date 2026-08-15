CREATE TABLE inventory_stock (
    sku                VARCHAR(100) PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stock_reservations (
    id         UUID PRIMARY KEY,
    order_id   UUID NOT NULL UNIQUE,
    status     VARCHAR(16) NOT NULL CHECK (status IN ('RESERVED', 'REJECTED', 'CONFIRMED', 'RELEASED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reservation_items (
    reservation_id UUID NOT NULL REFERENCES stock_reservations (id),
    sku            VARCHAR(100) NOT NULL,
    quantity       INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, sku)
);

CREATE INDEX idx_stock_reservations_expiry ON stock_reservations (status, expires_at);

INSERT INTO inventory_stock (sku, available_quantity) VALUES
    ('SKU-1', 100),
    ('SKU-2', 100),
    ('SKU-3', 100);
