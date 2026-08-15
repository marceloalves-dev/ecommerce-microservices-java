package com.ecom.order.domain.model;

import com.ecom.order.domain.exception.IllegalStateTransitionException;
import com.ecom.order.domain.exception.InvalidOrderException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz do pedido. DomInio puro — sem JPA, sem Spring.
 *
 * <p>Transicoes de estado <b>so</b> via metodos de domInio ({@link #awaitPayment()},
 * {@link #confirm()}, {@link #cancel(String)}, {@link #reject(String)}). Nao existe
 * {@code setStatus()}: cada metodo valida a transicao e lanca
 * {@link IllegalStateTransitionException} se invalida.
 */
public class Order {

    public static final int MAX_ITEMS = 100;
    public static final int MAX_REASON_LENGTH = 500;

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final CurrencyCode currency;
    private OrderStatus status;
    private UUID reservationId;
    private String cancellationReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Order(UUID id, UUID customerId, List<OrderItem> items, BigDecimal totalAmount,
                  CurrencyCode currency, OrderStatus status, UUID reservationId, String cancellationReason,
                  Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.status = status;
        this.reservationId = reservationId;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /** Cria um pedido novo em {@link OrderStatus#PENDING}. */
    public static Order create(UUID customerId, List<OrderItem> items, CurrencyCode currency) {
        return create(UUID.randomUUID(), customerId, items, currency);
    }

    /** Cria com uma identidade previamente alocada para tornar a saga reexecutavel. */
    public static Order create(UUID id, UUID customerId, List<OrderItem> items, CurrencyCode currency) {
        if (id == null) {
            throw new InvalidOrderException("id obrigatorio");
        }
        if (customerId == null) {
            throw new InvalidOrderException("customerId obrigatorio");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("pedido precisa de ao menos 1 item");
        }
        if (items.size() > MAX_ITEMS) {
            throw new InvalidOrderException("pedido deve ter no maximo 100 itens");
        }
        if (items.stream().anyMatch(item -> item == null)) {
            throw new InvalidOrderException("item do pedido nao pode ser nulo");
        }
        List<OrderItem> copy = List.copyOf(items);
        var skus = new HashSet<String>();
        for (OrderItem item : copy) {
            if (!skus.add(item.sku())) {
                throw new InvalidOrderException("sku duplicado no pedido: " + item.sku());
            }
        }
        BigDecimal total = copy.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.precision() > 19) {
            throw new InvalidOrderException("totalAmount excede o limite monetario");
        }
        Instant now = Instant.now();
        return new Order(id, customerId, copy, total,
                requireCurrency(currency),
                OrderStatus.PENDING, null, null, now, now, null);
    }

    private static CurrencyCode requireCurrency(CurrencyCode currency) {
        if (currency == null) {
            throw new InvalidOrderException("currency obrigatoria");
        }
        return currency;
    }

    /** Reconstroi um pedido a partir da persistencia (nao aplica regras de criacao). */
    public static Order reconstitute(UUID id, UUID customerId, List<OrderItem> items,
                                     BigDecimal totalAmount, CurrencyCode currency,
                                     OrderStatus status,
                                     UUID reservationId,
                                     String cancellationReason,
                                     Instant createdAt, Instant updatedAt, Long version) {
        return new Order(id, customerId, List.copyOf(items), totalAmount,
                currency, status, reservationId, cancellationReason, createdAt, updatedAt, version);
    }

    // ---- Transicoes da saga ----

    /** PENDING -> AWAITING_PAYMENT (estoque reservado, aguardando pagamento). */
    public void awaitPayment() {
        transitionTo(OrderStatus.AWAITING_PAYMENT, OrderStatus.PENDING);
    }

    /** PENDING -> AWAITING_PAYMENT, vinculando a reserva que protege os itens. */
    public void awaitPayment(UUID reservationId) {
        if (reservationId == null) {
            throw new InvalidOrderException("reservationId obrigatorio");
        }
        transitionTo(OrderStatus.AWAITING_PAYMENT, OrderStatus.PENDING);
        this.reservationId = reservationId;
    }

    /** AWAITING_PAYMENT -> CONFIRMED (pagamento aprovado). */
    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED, OrderStatus.AWAITING_PAYMENT);
    }

    /** AWAITING_PAYMENT -> CANCELLED (pagamento recusado / timeout — compensacao). */
    public void cancel(String reason) {
        validateReason(reason);
        transitionTo(OrderStatus.CANCELLED, OrderStatus.AWAITING_PAYMENT);
        this.cancellationReason = reason.trim();
    }

    /** PENDING -> REJECTED (sem estoque — fim da saga). */
    public void reject(String reason) {
        validateReason(reason);
        transitionTo(OrderStatus.REJECTED, OrderStatus.PENDING);
        this.cancellationReason = reason.trim();
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidOrderException("motivo obrigatorio");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidOrderException("motivo deve ter no maximo 500 caracteres");
        }
    }

    private void transitionTo(OrderStatus target, OrderStatus... allowedFrom) {
        for (OrderStatus from : allowedFrom) {
            if (this.status == from) {
                this.status = target;
                this.updatedAt = Instant.now();
                return;
            }
        }
        throw new IllegalStateTransitionException(this.status, target);
    }

    // ---- Getters (sem setters de status) ----

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public List<OrderItem> items() {
        return items;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public CurrencyCode currency() {
        return currency;
    }

    public OrderStatus status() {
        return status;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public String cancellationReason() {
        return cancellationReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
    }
}
