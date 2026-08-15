package com.ecom.order.application.usecase;

import com.ecom.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.ecom.order.application.port.out.IdempotencyRepository;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.application.port.out.PricingPort;
import com.ecom.order.application.port.out.InventoryPort;
import com.ecom.order.application.port.out.OrderEventPublisher;
import com.ecom.order.domain.exception.IdempotencyConflictException;
import com.ecom.order.domain.model.CurrencyCode;
import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    OrderRepository orders;

    @Mock
    IdempotencyRepository idempotency;

    @Mock
    PricingPort pricing;

    @Mock
    InventoryPort inventory;

    @Mock
    OrderEventPublisher events;

    CreateOrderService service;

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(orders, idempotency, pricing, inventory, events);
    }

    @Test
    void cria_com_preco_confiavel_e_conclui_claim() {
        UUID claimId = UUID.randomUUID();
        CreateOrderCommand command = command("idem-1", 2);
        when(idempotency.claim(eq(command.customerId()), eq("idem-1"), anyString()))
                .thenAnswer(invocation -> new IdempotencyRepository.Claim(
                        claimId, invocation.getArgument(2), null));
        when(pricing.getPrice("SKU-1"))
                .thenReturn(new PricingPort.Price(new BigDecimal("10.00"), CurrencyCode.BRL));
        when(inventory.reserve(any(), any())).thenReturn(new InventoryPort.Reservation(UUID.randomUUID(), true));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = service.create(command);

        assertThat(created.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(created.currency()).isEqualTo(CurrencyCode.BRL);
        verify(idempotency).complete(claimId, created.id());
    }

    @Test
    void repeticao_da_mesma_requisicao_retorna_pedido_original() {
        UUID orderId = UUID.randomUUID();
        CreateOrderCommand command = command("idem-2", 1);
        Order existing = Order.reconstitute(
                orderId,
                command.customerId(),
                List.of(new OrderItem("SKU-1", 1, new BigDecimal("10.00"))),
                new BigDecimal("10.00"),
                CurrencyCode.BRL,
                com.ecom.order.domain.model.OrderStatus.PENDING,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now(),
                0L);
        when(idempotency.claim(eq(command.customerId()), eq("idem-2"), anyString()))
                .thenAnswer(invocation -> new IdempotencyRepository.Claim(
                        UUID.randomUUID(), invocation.getArgument(2), orderId));
        when(orders.findById(orderId)).thenReturn(Optional.of(existing));

        Order replayed = service.create(command);

        assertThat(replayed.id()).isEqualTo(orderId);
        verify(pricing, never()).getPrice(anyString());
        verify(orders, never()).save(any());
    }

    @Test
    void mesma_chave_com_conteudo_diferente_retorna_conflito() {
        CreateOrderCommand command = command("idem-3", 1);
        when(idempotency.claim(eq(command.customerId()), eq("idem-3"), anyString()))
                .thenReturn(new IdempotencyRepository.Claim(
                        UUID.randomUUID(), "outro-hash", UUID.randomUUID()));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(pricing, never()).getPrice(anyString());
    }

    @Test
    void hash_nao_muda_quando_itens_apenas_trocam_de_ordem() {
        UUID customerId = UUID.randomUUID();
        var first = new CreateOrderCommand(customerId, "key", List.of(
                new CreateOrderCommand.Line("SKU-1", 1),
                new CreateOrderCommand.Line("SKU-2", 2)));
        var reordered = new CreateOrderCommand(customerId, "key", List.of(
                new CreateOrderCommand.Line("SKU-2", 2),
                new CreateOrderCommand.Line("SKU-1", 1)));

        assertThat(OrderRequestHasher.hash(first))
                .isEqualTo(OrderRequestHasher.hash(reordered));
    }

    private static CreateOrderCommand command(String key, int quantity) {
        return new CreateOrderCommand(
                UUID.randomUUID(),
                key,
                List.of(new CreateOrderCommand.Line("SKU-1", quantity)));
    }
}
