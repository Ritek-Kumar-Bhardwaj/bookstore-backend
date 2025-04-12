package com.bookstore.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderRequestDTO {
    private int userId;
    private double totalAmount;
    private String paymentMethod;
    private List<OrderItemDTO> items;

    // Getters and Setters

    public static class OrderItemDTO {
        private int bookId;
        private int quantity;
        private double price;

        // Getters and Setters
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}

