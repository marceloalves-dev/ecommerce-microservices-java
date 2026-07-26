package com.ecom.order.api;

import com.ecom.order.api.dto.CreateOrderRequest;
import com.ecom.order.api.dto.OrderListResponse;
import com.ecom.order.api.dto.OrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração ponta a ponta: HTTP -> use case -> JPA -> Postgres real
 * (Testcontainers), incluindo Flyway, idempotência e paginação.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@Testcontainers
class OrderControllerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate rest;

    @Test
    void cria_com_preco_do_catalogo_e_recupera_pedido() {
        var request = request(UUID.randomUUID(),
                new CreateOrderRequest.Line("SKU-1", 2),
                new CreateOrderRequest.Line("SKU-2", 3));

        ResponseEntity<OrderResponse> created = create("create-fetch-" + UUID.randomUUID(), request);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getHeaders().getLocation().toString())
                .isEqualTo("/api/v1/orders/" + created.getBody().id());
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().status().name()).isEqualTo("PENDING");
        assertThat(created.getBody().currency().name()).isEqualTo("BRL");
        assertThat(created.getBody().totalAmount()).isEqualByComparingTo("32.00");

        UUID id = created.getBody().id();
        ResponseEntity<OrderResponse> fetched =
                rest.getForEntity("/api/v1/orders/" + id, OrderResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().id()).isEqualTo(id);
        assertThat(fetched.getBody().items()).hasSize(2);
    }

    @Test
    void repeticao_com_mesma_chave_retorna_o_mesmo_pedido() {
        UUID customerId = UUID.randomUUID();
        var request = request(customerId, new CreateOrderRequest.Line("SKU-1", 1));
        String key = "replay-" + UUID.randomUUID();

        ResponseEntity<OrderResponse> first = create(key, request);
        ResponseEntity<OrderResponse> replay = create(key, request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getBody()).isNotNull();
        assertThat(replay.getBody().id()).isEqualTo(first.getBody().id());
    }

    @Test
    void chamadas_concorrentes_com_mesma_chave_criam_um_unico_pedido() throws Exception {
        var request = request(
                UUID.randomUUID(), new CreateOrderRequest.Line("SKU-1", 1));
        String key = "concurrent-" + UUID.randomUUID();
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return create(key, request);
            });
            var second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return create(key, request);
            });
            start.countDown();

            ResponseEntity<OrderResponse> firstResponse = first.get(10, TimeUnit.SECONDS);
            ResponseEntity<OrderResponse> secondResponse = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(secondResponse.getBody().id()).isEqualTo(firstResponse.getBody().id());
        }
    }

    @Test
    void mesma_chave_com_payload_diferente_retorna_409() {
        UUID customerId = UUID.randomUUID();
        String key = "conflict-" + UUID.randomUUID();
        create(key, request(customerId, new CreateOrderRequest.Line("SKU-1", 1)));

        ResponseEntity<String> conflict = createRaw(
                key, request(customerId, new CreateOrderRequest.Line("SKU-1", 2)));

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).contains("idempotency-conflict");
    }

    @Test
    void listagem_e_paginada_e_nao_materializa_itens() {
        create("page-1-" + UUID.randomUUID(),
                request(UUID.randomUUID(), new CreateOrderRequest.Line("SKU-1", 1)));
        create("page-2-" + UUID.randomUUID(),
                request(UUID.randomUUID(), new CreateOrderRequest.Line("SKU-2", 1)));

        ResponseEntity<OrderListResponse> response =
                rest.getForEntity("/api/v1/orders?page=0&size=1", OrderListResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().hasNext()).isTrue();
    }

    @Test
    void sku_duplicado_retorna_400() {
        var request = request(UUID.randomUUID(),
                new CreateOrderRequest.Line("SKU-1", 1),
                new CreateOrderRequest.Line("SKU-1", 2));

        ResponseEntity<String> response =
                createRaw("duplicate-sku-" + UUID.randomUUID(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("sku duplicado");
    }

    @Test
    void sku_desconhecido_retorna_400() {
        var request = request(
                UUID.randomUUID(), new CreateOrderRequest.Line("NAO-EXISTE", 1));

        ResponseEntity<String> response =
                createRaw("unknown-sku-" + UUID.randomUUID(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("sku desconhecido");
    }

    @Test
    void pedido_inexistente_retorna_404() {
        ResponseEntity<String> response =
                rest.getForEntity("/api/v1/orders/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requisicao_invalida_retorna_400() {
        var invalid = new CreateOrderRequest(null, List.of());

        ResponseEntity<String> response =
                createRaw("invalid-" + UUID.randomUUID(), invalid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pagina_maior_que_limite_retorna_400() {
        ResponseEntity<String> response =
                rest.getForEntity("/api/v1/orders?page=0&size=101", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void corpo_acima_do_limite_retorna_413() {
        String body = "{\"padding\":\"" + "x".repeat(140_000) + "\"}";
        HttpHeaders headers = headers("large-" + UUID.randomUUID());

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/orders", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    private ResponseEntity<OrderResponse> create(String key, CreateOrderRequest request) {
        return rest.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers(key)),
                OrderResponse.class);
    }

    private ResponseEntity<String> createRaw(String key, CreateOrderRequest request) {
        return rest.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers(key)),
                String.class);
    }

    private static CreateOrderRequest request(
            UUID customerId, CreateOrderRequest.Line... lines) {
        return new CreateOrderRequest(customerId, List.of(lines));
    }

    private static HttpHeaders headers(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        return headers;
    }
}
