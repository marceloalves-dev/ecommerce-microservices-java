package com.ecom.order.infrastructure.grpc;

import com.ecom.inventory.grpc.v1.InventoryServiceGrpc;
import com.ecom.inventory.grpc.v1.Item;
import com.ecom.inventory.grpc.v1.ReservationStatus;
import com.ecom.inventory.grpc.v1.ReserveStockRequest;
import com.ecom.order.application.port.out.InventoryPort;
import com.ecom.order.domain.exception.InventoryUnavailableException;
import com.ecom.order.domain.model.OrderItem;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
class InventoryGrpcAdapter implements InventoryPort {
    private final InventoryServiceGrpc.InventoryServiceBlockingStub client;
    private final long deadlineMillis;

    InventoryGrpcAdapter(InventoryServiceGrpc.InventoryServiceBlockingStub client,
                         @Value("${ecom.inventory.deadline-ms:1500}") long deadlineMillis) {
        this.client = client;
        this.deadlineMillis = deadlineMillis;
    }

    @Override
    public Reservation reserve(UUID orderId, List<OrderItem> items) {
        try {
            var response = client.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                    .reserveStock(ReserveStockRequest.newBuilder().setOrderId(orderId.toString())
                            .addAllItems(items.stream().map(item -> Item.newBuilder().setSku(item.sku())
                                    .setQuantity(item.quantity()).build()).toList())
                            .build());
            return new Reservation(UUID.fromString(response.getReservationId()),
                    response.getStatus() == ReservationStatus.RESERVATION_STATUS_CONFIRMED,
                    Instant.ofEpochSecond(response.getExpiresAt().getSeconds(), response.getExpiresAt().getNanos()));
        } catch (StatusRuntimeException ex) {
            Status.Code code = ex.getStatus().getCode();
            if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
                throw new InventoryUnavailableException("estoque temporariamente indisponivel", ex);
            }
            throw ex;
        }
    }
}

@Configuration
class InventoryGrpcClientConfiguration {
    @Bean(destroyMethod = "shutdownNow")
    ManagedChannel inventoryChannel(@Value("${ecom.inventory.host:localhost}") String host,
                                    @Value("${ecom.inventory.port:9091}") int port) {
        return io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    @Bean
    InventoryServiceGrpc.InventoryServiceBlockingStub inventoryClient(ManagedChannel inventoryChannel) {
        return InventoryServiceGrpc.newBlockingStub(inventoryChannel);
    }
}
