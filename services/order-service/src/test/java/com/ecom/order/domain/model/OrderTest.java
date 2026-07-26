package com.ecom.order.domain.model;

import com.ecom.order.domain.exception.IllegalStateTransitionException;
import com.ecom.order.domain.exception.InvalidOrderException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static Order novoPedido() {
        return Order.create(UUID.randomUUID(), List.of(
                new OrderItem("SKU-1", 2, new BigDecimal("10.00")),
                new OrderItem("SKU-2", 1, new BigDecimal("5.50"))),
                CurrencyCode.BRL);
    }

    @Test
    void cria_pedido_em_pending_e_calcula_total() {
        Order order = novoPedido();

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo("25.50");
        assertThat(order.currency()).isEqualTo(CurrencyCode.BRL);
        assertThat(order.items()).hasSize(2);
    }

    @Test
    void fluxo_feliz_pending_awaiting_confirmed() {
        Order order = novoPedido();

        order.awaitPayment();
        assertThat(order.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);

        order.confirm();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelamento_com_compensacao_guarda_motivo() {
        Order order = novoPedido();
        order.awaitPayment();

        order.cancel("pagamento recusado");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellationReason()).isEqualTo("pagamento recusado");
    }

    @Test
    void rejeita_quando_sem_estoque_direto_de_pending() {
        Order order = novoPedido();

        order.reject("sem estoque");

        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void confirmar_sem_passar_por_awaiting_payment_falha() {
        Order order = novoPedido();

        assertThatThrownBy(order::confirm)
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void pedido_sem_itens_e_invalido() {
        assertThatThrownBy(() -> Order.create(
                UUID.randomUUID(), List.of(), CurrencyCode.BRL))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void sku_duplicado_e_invalido() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), List.of(
                new OrderItem("SKU-1", 1, BigDecimal.ONE),
                new OrderItem("SKU-1", 2, BigDecimal.ONE)), CurrencyCode.BRL))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("sku duplicado");
    }

    @Test
    void pedido_limita_quantidade_de_itens() {
        List<OrderItem> items = java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(i -> new OrderItem("SKU-" + i, 1, BigDecimal.ONE))
                .toList();

        assertThatThrownBy(() -> Order.create(
                UUID.randomUUID(), items, CurrencyCode.BRL))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("100 itens");
    }

    @Test
    void preco_com_mais_de_duas_casas_e_invalido() {
        assertThatThrownBy(() -> new OrderItem("SKU-1", 1, new BigDecimal("1.001")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("2 casas");
    }

    @Test
    void itens_sao_copiados_e_expostos_como_lista_imutavel() {
        var source = new ArrayList<OrderItem>();
        source.add(new OrderItem("SKU-1", 1, BigDecimal.ONE));

        Order order = Order.create(UUID.randomUUID(), source, CurrencyCode.BRL);
        source.add(new OrderItem("SKU-2", 1, BigDecimal.ONE));

        assertThat(order.items()).hasSize(1);
        assertThatThrownBy(() -> order.items().add(
                new OrderItem("SKU-3", 1, BigDecimal.ONE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void motivo_invalido_nao_altera_estado() {
        Order order = novoPedido();
        order.awaitPayment();

        assertThatThrownBy(() -> order.cancel(" "))
                .isInstanceOf(InvalidOrderException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }
}
