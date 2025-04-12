package com.bookstore.api.services;

import com.bookstore.api.dto.OrderRequest;
import com.bookstore.api.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> createOrder(OrderRequest request) {
        return orderRepository.insertOrder(request);
    }
}
