package com.ecom.order.infrastructure.persistence;

import com.ecom.order.domain.model.OrderStatus;
import com.ecom.order.domain.model.CurrencyCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA do pedido. Deliberadamente sem Lombok {@code @Data}:
 * {@code equals}/{@code hashCode} sao baseados so no id (estavel com proxy do Hibernate).
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<OrderItemEntity> items = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    protected OrderEntity() {
        // exigido pelo JPA
    }

    public OrderEntity(UUID id, UUID customerId, OrderStatus status, BigDecimal totalAmount,
                       CurrencyCode currency, UUID reservationId, Instant reservationExpiresAt, String cancellationReason,
                       Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.reservationId = reservationId;
        this.reservationExpiresAt = reservationExpiresAt;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        item.setLineNumber(this.items.size());
        this.items.add(item);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public Long getVersion() {
        return version;
    }

    public void updateFrom(com.ecom.order.domain.model.Order order) {
        this.status = order.status();
        this.totalAmount = order.totalAmount();
        this.currency = order.currency();
        this.reservationId = order.reservationId();
        this.reservationExpiresAt = order.reservationExpiresAt();
        this.cancellationReason = order.cancellationReason();
        this.updatedAt = order.updatedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderEntity that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
