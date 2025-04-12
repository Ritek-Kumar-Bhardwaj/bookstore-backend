package com.bookstore.api.repositories;

import com.bookstore.api.models.Book;
import com.bookstore.api.dto.SearchAndFilter;

import java.util.List;
import java.util.Map;

public interface BookRepositoryInterface {
    int save(Book book);
    boolean deleteBook(int bookId);
    boolean updateBook(Book book);
    List<Book> findAllBooks();
    List<Book> searchBooks(SearchAndFilter filter);
    int getTotalBookCount();
    boolean existsById(int bookId);
    Map<Integer, String> getBookCoverPaths(List<Book> books);
}

