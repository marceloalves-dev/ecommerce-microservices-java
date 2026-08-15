package com.ecom.inventory.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import org.flywaydb.core.Flyway;

@Factory
class DatabaseFactory {
    @Bean(preDestroy = "close")
    @Context
    HikariDataSource dataSource(
            @Value("${datasources.default.url}") String url,
            @Value("${datasources.default.username}") String username,
            @Value("${datasources.default.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    @Context
    Flyway migrate(HikariDataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/inventory/migration").load();
        flyway.migrate();
        return flyway;
    }
}
