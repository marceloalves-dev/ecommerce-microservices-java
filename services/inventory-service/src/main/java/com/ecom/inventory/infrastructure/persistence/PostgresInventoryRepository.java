package com.ecom.inventory.infrastructure.persistence;

import com.ecom.inventory.application.port.in.InventoryUseCase;
import com.ecom.inventory.application.port.out.InventoryRepository;
import com.ecom.inventory.domain.model.Reservation;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Adapter PostgreSQL. As linhas de estoque sao bloqueadas antes do decremento. */
@Singleton
class PostgresInventoryRepository implements InventoryRepository {
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);
    private final DataSource dataSource;

    PostgresInventoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public InventoryUseCase.Availability check(List<InventoryUseCase.Line> lines) {
        List<String> unavailable = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            for (InventoryUseCase.Line line : lines) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT available_quantity FROM inventory_stock WHERE sku = ?")) {
                    statement.setString(1, line.sku().trim());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || result.getInt(1) < line.quantity()) {
                            unavailable.add(line.sku().trim());
                        }
                    }
                }
            }
            return new InventoryUseCase.Availability(unavailable.isEmpty(), List.copyOf(unavailable));
        } catch (SQLException ex) {
            throw new InventoryPersistenceException(ex);
        }
    }

    @Override
    public Reservation reserve(UUID orderId, List<InventoryUseCase.Line> lines) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation existing = findReservation(connection, orderId);
                if (existing != null) {
                    connection.commit();
                    return existing;
                }

                boolean available = lockAndVerifyAvailability(connection, lines);
                Instant expiresAt = Instant.now().plus(RESERVATION_TTL);
                Reservation reservation = new Reservation(UUID.randomUUID(), orderId,
                        available ? Reservation.Status.RESERVED : Reservation.Status.REJECTED, expiresAt);
                insertReservation(connection, reservation, lines);
                if (available) {
                    decrementStock(connection, lines);
                }
                connection.commit();
                return reservation;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new InventoryPersistenceException(ex);
        }
    }

    @Override
    public void confirm(UUID reservationId) {
        changeReservation(reservationId, true);
    }

    @Override
    public void release(UUID reservationId) {
        changeReservation(reservationId, false);
    }

    private void changeReservation(UUID reservationId, boolean confirm) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = findReservationByIdForUpdate(connection, reservationId);
                if (reservation == null) {
                    throw new IllegalArgumentException("reserva inexistente");
                }
                if (reservation.status() == Reservation.Status.REJECTED
                        || reservation.status() == (confirm ? Reservation.Status.CONFIRMED : Reservation.Status.RELEASED)) {
                    connection.commit();
                    return;
                }
                if (reservation.status() != Reservation.Status.RESERVED) {
                    throw new IllegalStateException("transicao de reserva invalida: " + reservation.status());
                }
                if (!confirm) {
                    restoreStock(connection, reservationId);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE stock_reservations SET status = ? WHERE id = ?")) {
                    statement.setString(1, confirm ? Reservation.Status.CONFIRMED.name() : Reservation.Status.RELEASED.name());
                    statement.setObject(2, reservationId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new InventoryPersistenceException(ex);
        }
    }

    private static Reservation findReservationByIdForUpdate(Connection connection, UUID reservationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT order_id, status, expires_at FROM stock_reservations WHERE id = ? FOR UPDATE")) {
            statement.setObject(1, reservationId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new Reservation(reservationId, (UUID) result.getObject("order_id"),
                        Reservation.Status.valueOf(result.getString("status")), result.getTimestamp("expires_at").toInstant());
            }
        }
    }

    private static void restoreStock(Connection connection, UUID reservationId) throws SQLException {
        try (PreparedStatement items = connection.prepareStatement(
                "SELECT sku, quantity FROM reservation_items WHERE reservation_id = ?")) {
            items.setObject(1, reservationId);
            try (ResultSet result = items.executeQuery()) {
                while (result.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE inventory_stock SET available_quantity = available_quantity + ?, updated_at = now() WHERE sku = ?")) {
                        update.setInt(1, result.getInt("quantity"));
                        update.setString(2, result.getString("sku"));
                        update.executeUpdate();
                    }
                }
            }
        }
    }

    private static Reservation findReservation(Connection connection, UUID orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, status, expires_at FROM stock_reservations WHERE order_id = ?")) {
            statement.setObject(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Reservation((UUID) result.getObject("id"), orderId,
                        Reservation.Status.valueOf(result.getString("status")),
                        result.getTimestamp("expires_at").toInstant());
            }
        }
    }

    private static boolean lockAndVerifyAvailability(Connection connection, List<InventoryUseCase.Line> lines)
            throws SQLException {
        boolean available = true;
        for (InventoryUseCase.Line line : lines) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT available_quantity FROM inventory_stock WHERE sku = ? FOR UPDATE")) {
                statement.setString(1, line.sku().trim());
                try (ResultSet result = statement.executeQuery()) {
                    available &= result.next() && result.getInt(1) >= line.quantity();
                }
            }
        }
        return available;
    }

    private static void decrementStock(Connection connection, List<InventoryUseCase.Line> lines) throws SQLException {
        for (InventoryUseCase.Line line : lines) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE inventory_stock SET available_quantity = available_quantity - ?, updated_at = now() WHERE sku = ?")) {
                statement.setInt(1, line.quantity());
                statement.setString(2, line.sku().trim());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("SKU desapareceu durante a reserva: " + line.sku());
                }
            }
        }
    }

    private static void insertReservation(Connection connection, Reservation reservation,
                                          List<InventoryUseCase.Line> lines) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO stock_reservations (id, order_id, status, expires_at, created_at) VALUES (?, ?, ?, ?, now())")) {
            statement.setObject(1, reservation.id());
            statement.setObject(2, reservation.orderId());
            statement.setString(3, reservation.status().name());
            statement.setTimestamp(4, Timestamp.from(reservation.expiresAt()));
            statement.executeUpdate();
        }
        for (InventoryUseCase.Line line : lines) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO reservation_items (reservation_id, sku, quantity) VALUES (?, ?, ?)")) {
                statement.setObject(1, reservation.id());
                statement.setString(2, line.sku().trim());
                statement.setInt(3, line.quantity());
                statement.executeUpdate();
            }
        }
    }

    private static final class InventoryPersistenceException extends RuntimeException {
        private InventoryPersistenceException(SQLException cause) {
            super("falha ao acessar o estoque", cause);
        }
    }
}
