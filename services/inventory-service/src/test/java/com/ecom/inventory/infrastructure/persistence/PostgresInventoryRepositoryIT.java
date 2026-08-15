package com.ecom.inventory.infrastructure.persistence;

import com.ecom.inventory.application.port.in.InventoryUseCase;
import com.ecom.inventory.domain.model.Reservation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercita bloqueio/decremento no PostgreSQL real, nao um mock de repositorio. */
@Testcontainers
class PostgresInventoryRepositoryIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private HikariDataSource dataSource;
    private PostgresInventoryRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        dataSource = new HikariDataSource(config);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/inventory/migration").load().migrate();
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE reservation_items, stock_reservations, inventory_stock");
            statement.executeUpdate("INSERT INTO inventory_stock (sku, available_quantity) VALUES ('SKU-1', 10), ('SKU-2', 5)");
        }
        repository = new PostgresInventoryRepository(dataSource);
    }

    @AfterEach
    void close() {
        dataSource.close();
    }

    @Test
    void reserva_e_idempotente_e_nao_permite_estoque_negativo() {
        UUID orderId = UUID.randomUUID();
        var lines = List.of(new InventoryUseCase.Line("SKU-1", 7));

        Reservation first = repository.reserve(orderId, lines);
        Reservation replay = repository.reserve(orderId, lines);
        Reservation rejected = repository.reserve(UUID.randomUUID(), List.of(new InventoryUseCase.Line("SKU-1", 4)));

        assertThat(first.status()).isEqualTo(Reservation.Status.RESERVED);
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(rejected.status()).isEqualTo(Reservation.Status.REJECTED);
        assertThat(repository.check(List.of(new InventoryUseCase.Line("SKU-1", 4))).available()).isFalse();
    }

    @Test
    void liberar_reserva_restitui_o_estoque_uma_unica_vez() {
        Reservation reservation = repository.reserve(UUID.randomUUID(), List.of(new InventoryUseCase.Line("SKU-2", 5)));

        repository.release(reservation.id());
        repository.release(reservation.id());

        assertThat(repository.check(List.of(new InventoryUseCase.Line("SKU-2", 5))).available()).isTrue();
    }

    @Test
    void libera_reservas_expiradas_sem_restituir_o_estoque_duas_vezes() throws Exception {
        Reservation reservation = repository.reserve(UUID.randomUUID(), List.of(new InventoryUseCase.Line("SKU-1", 6)));
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "UPDATE stock_reservations SET expires_at = ? WHERE id = ?")) {
            statement.setTimestamp(1, java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
            statement.setObject(2, reservation.id());
            statement.executeUpdate();
        }

        assertThat(repository.releaseExpiredReservations(Instant.now(), 100)).isEqualTo(1);
        assertThat(repository.releaseExpiredReservations(Instant.now(), 100)).isZero();
        assertThat(repository.check(List.of(new InventoryUseCase.Line("SKU-1", 10))).available()).isTrue();
    }

    @Test
    void inbox_processa_confirmacao_uma_unica_vez() throws Exception {
        Reservation reservation = repository.reserve(UUID.randomUUID(), List.of(new InventoryUseCase.Line("SKU-1", 2)));
        UUID eventId = UUID.randomUUID();

        assertThat(repository.confirmOnce("inventory-order-lifecycle", eventId, reservation.id())).isTrue();
        assertThat(repository.confirmOnce("inventory-order-lifecycle", eventId, reservation.id())).isFalse();
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
                "SELECT status FROM stock_reservations WHERE id = ?")) {
            statement.setObject(1, reservation.id());
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("status")).isEqualTo("CONFIRMED");
            }
        }
    }
}
