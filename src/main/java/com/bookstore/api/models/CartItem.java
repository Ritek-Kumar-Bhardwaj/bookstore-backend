package com.bookstore.api.models;

public class CartItem {

    private int cartItemId;
    private int cartId;
    private int bookId;
    private int quantity;
    private double price;
    private java.sql.Timestamp addedAt;

    // Getters and Setters

    public int getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(int cartItemId) {
        this.cartItemId = cartItemId;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public java.sql.Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(java.sql.Timestamp addedAt) {
        this.addedAt = addedAt;
    }
}

