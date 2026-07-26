package com.ecom.order.api.controller;

import com.ecom.order.api.dto.CreateOrderRequest;
import com.ecom.order.api.dto.OrderListResponse;
import com.ecom.order.api.dto.OrderResponse;
import com.ecom.order.application.port.in.CreateOrderUseCase;
import com.ecom.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.ecom.order.application.port.in.GetOrderUseCase;
import com.ecom.order.domain.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
class OrderController {

    private final CreateOrderUseCase createOrder;
    private final GetOrderUseCase getOrder;

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        var lines = request.items().stream()
                .map(l -> new CreateOrderCommand.Line(l.sku(), l.quantity()))
                .toList();
        Order order = createOrder.create(
                new CreateOrderCommand(request.customerId(), idempotencyKey, lines));

        URI location = URI.create("/api/v1/orders/" + order.id());
        return ResponseEntity.created(location).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    OrderResponse getById(@PathVariable UUID id) {
        return OrderResponse.from(getOrder.getById(id));
    }

    @GetMapping
    OrderListResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return OrderListResponse.from(getOrder.list(page, size));
    }
}
