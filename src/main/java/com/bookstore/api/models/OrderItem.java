package com.bookstore.api.models;

import java.math.BigDecimal;

public record OrderItem(
        int bookId,
        int quantity,
        double price
) {}
