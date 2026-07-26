package com.ecom.order.application.usecase;

import com.ecom.order.application.port.in.CreateOrderUseCase;
import com.ecom.order.application.port.out.IdempotencyRepository;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.application.port.out.PricingPort;
import com.ecom.order.domain.exception.IdempotencyConflictException;
import com.ecom.order.domain.exception.InvalidOrderException;
import com.ecom.order.domain.model.Order;
import com.ecom.order.domain.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final PricingPort pricingPort;

    @Override
    @Transactional
    public Order create(CreateOrderCommand command) {
        if (command == null || command.customerId() == null
                || command.items() == null || command.items().isEmpty()) {
            throw new InvalidOrderException("customerId e items sao obrigatorios");
        }
        if (command.items().size() > Order.MAX_ITEMS) {
            throw new InvalidOrderException("pedido deve ter no maximo 100 itens");
        }
        for (CreateOrderCommand.Line line : command.items()) {
            if (line == null || line.sku() == null || line.sku().isBlank()) {
                throw new InvalidOrderException("sku obrigatorio");
            }
            if (line.sku().trim().length() > OrderItem.MAX_SKU_LENGTH) {
                throw new InvalidOrderException("sku deve ter no maximo 100 caracteres");
            }
            if (line.quantity() <= 0 || line.quantity() > OrderItem.MAX_QUANTITY) {
                throw new InvalidOrderException("quantity deve estar entre 1 e 10000");
            }
        }
        validateIdempotencyKey(command.idempotencyKey());
        String requestHash = OrderRequestHasher.hash(command);
        IdempotencyRepository.Claim claim = idempotencyRepository.claim(
                command.customerId(), command.idempotencyKey().trim(), requestHash);

        if (!claim.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
        if (claim.orderId() != null) {
            return orderRepository.findById(claim.orderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "registro de idempotencia aponta para pedido inexistente"));
        }

        var prices = command.items().stream()
                .map(line -> new PricedLine(line, pricingPort.getPrice(line.sku())))
                .toList();
        var currencies = prices.stream().map(p -> p.price().currency()).distinct().toList();
        if (currencies.size() != 1) {
            throw new InvalidOrderException("todos os itens devem usar a mesma moeda");
        }
        List<OrderItem> items = prices.stream()
                .map(priced -> new OrderItem(
                        priced.line().sku(),
                        priced.line().quantity(),
                        priced.price().amount()))
                .toList();
        Order order = Order.create(command.customerId(), items, currencies.getFirst());
        Order saved = orderRepository.save(order);
        idempotencyRepository.complete(claim.id(), saved.id());
        return saved;
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidOrderException("Idempotency-Key obrigatoria");
        }
        if (key.trim().length() > 100) {
            throw new InvalidOrderException("Idempotency-Key deve ter no maximo 100 caracteres");
        }
    }

    private record PricedLine(CreateOrderCommand.Line line, PricingPort.Price price) {
    }
}
