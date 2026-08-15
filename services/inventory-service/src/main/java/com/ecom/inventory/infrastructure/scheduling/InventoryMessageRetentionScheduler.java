package com.ecom.inventory.infrastructure.scheduling;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.time.Instant;

@Singleton
public class InventoryMessageRetentionScheduler {
    private final DataSource dataSource;
    private final int retentionDays;

    public InventoryMessageRetentionScheduler(DataSource dataSource,
                                              @Value("${inventory.messaging.retention-days:30}") int retentionDays) {
        this.dataSource = dataSource;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelay = "${inventory.messaging.cleanup-fixed-delay:1h}")
    void clean() {
        try (var connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM inventory_processed_events WHERE processed_at < ?")) {
            statement.setTimestamp(1, java.sql.Timestamp.from(Instant.now().minusSeconds(retentionDays * 86_400L)));
            statement.executeUpdate();
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("falha ao limpar inbox do inventory", ex);
        }
    }
}
