package com.ecom.inventory.infrastructure.grpc;

import com.ecom.inventory.application.port.in.InventoryUseCase;
import com.ecom.inventory.domain.model.Reservation;
import com.ecom.inventory.grpc.v1.CheckAvailabilityRequest;
import com.ecom.inventory.grpc.v1.CheckAvailabilityResponse;
import com.ecom.inventory.grpc.v1.InventoryServiceGrpc;
import com.ecom.inventory.grpc.v1.ReservationStatus;
import com.ecom.inventory.grpc.v1.ReserveStockRequest;
import com.ecom.inventory.grpc.v1.ReserveStockResponse;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.micronaut.grpc.annotation.GrpcService;

import java.util.UUID;

@GrpcService
public class InventoryGrpcEndpoint extends InventoryServiceGrpc.InventoryServiceImplBase {
    private final InventoryUseCase inventory;

    public InventoryGrpcEndpoint(InventoryUseCase inventory) {
        this.inventory = inventory;
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request,
                                  StreamObserver<CheckAvailabilityResponse> observer) {
        try {
            var availability = inventory.check(request.getItemsList().stream()
                    .map(item -> new InventoryUseCase.Line(item.getSku(), item.getQuantity())).toList());
            observer.onNext(CheckAvailabilityResponse.newBuilder()
                    .setAvailable(availability.available())
                    .addAllUnavailableSkus(availability.unavailableSkus())
                    .build());
            observer.onCompleted();
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (RuntimeException ex) {
            observer.onError(Status.INTERNAL.withDescription("erro ao consultar estoque").withCause(ex).asRuntimeException());
        }
    }

    @Override
    public void reserveStock(ReserveStockRequest request, StreamObserver<ReserveStockResponse> observer) {
        try {
            Reservation reservation = inventory.reserve(UUID.fromString(request.getOrderId()),
                    request.getItemsList().stream()
                            .map(item -> new InventoryUseCase.Line(item.getSku(), item.getQuantity())).toList());
            ReservationStatus status = reservation.status() == Reservation.Status.REJECTED
                    ? ReservationStatus.RESERVATION_STATUS_REJECTED
                    : ReservationStatus.RESERVATION_STATUS_CONFIRMED;
            observer.onNext(ReserveStockResponse.newBuilder()
                    .setReservationId(reservation.id().toString())
                    .setStatus(status)
                    .setExpiresAt(toProtoTimestamp(reservation.expiresAt()))
                    .build());
            observer.onCompleted();
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (RuntimeException ex) {
            observer.onError(Status.INTERNAL.withDescription("erro ao reservar estoque").withCause(ex).asRuntimeException());
        }
    }

    private static Timestamp toProtoTimestamp(java.time.Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
