package com.bookstore.api.dto;

import java.util.List;

public class LoginResponse {
    private String role;
    private int userId;
    private int cartId;
    private List<CartBookDetails> cartItems;

    // Getters and Setters only (no constructor)
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }

    public List<CartBookDetails> getCartItems() { return cartItems; }
    public void setCartItems(List<CartBookDetails> cartItems) { this.cartItems = cartItems; }
}
