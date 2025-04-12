package com.bookstore.api.dto;

import com.bookstore.api.models.Book;

public class CartBookDetails {
    private Book book;
    private int quantity;
    private double price;
    private String imageUrl;

    public CartBookDetails() {}

    // Getters and Setters
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
