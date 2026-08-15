CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    customer_id         UUID NOT NULL,
    status              VARCHAR(32) NOT NULL,
    total_amount        NUMERIC(19, 2) NOT NULL,
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku        VARCHAR(100) NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);
