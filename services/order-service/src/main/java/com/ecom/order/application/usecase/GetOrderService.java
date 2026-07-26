package com.ecom.order.application.usecase;

import com.ecom.order.application.port.in.GetOrderUseCase;
import com.ecom.order.application.port.out.OrderRepository;
import com.ecom.order.domain.exception.OrderNotFoundException;
import com.ecom.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    public Order getById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public OrderSlice list(int page, int size) {
        OrderRepository.OrderSlice slice = orderRepository.findSlice(page, size);
        return new OrderSlice(slice.content(), page, size, slice.hasNext());
    }
}
