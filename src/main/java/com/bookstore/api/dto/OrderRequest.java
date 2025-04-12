package com.bookstore.api.dto;

import com.bookstore.api.models.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        int userId,
        double totalAmount,
        String paymentMethod,
        String deliveryCode,
        List<OrderItem> items
) {}
