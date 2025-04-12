package com.bookstore.api.dto;

import com.bookstore.api.models.Book;
import java.util.List;

public class SearchResponse {
    private int total;
    private List<Book> results;

    // Constructors
    public SearchResponse(int total, List<Book> results) {
        this.total = total;
        this.results = results;
    }

    // Getters and Setters

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Book> getResults() {
        return results;
    }

    public void setResults(List<Book> results) {
        this.results = results;
    }
}
