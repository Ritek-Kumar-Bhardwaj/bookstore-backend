package com.bookstore.api.controllers;

import com.bookstore.api.dto.SearchAndFilter;
import com.bookstore.api.models.Book;
import com.bookstore.api.services.BookService;
import com.bookstore.api.security.JwtUtil;
import com.bookstore.api.utils.ImageUploader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ImageUploader imageUploader;

    @PostMapping("/add")
    public ResponseEntity<?> addBook(
            @RequestHeader("Authorization") String token,
            @RequestPart("book") String bookJson,  // Receive JSON as a String
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Book book = objectMapper.readValue(bookJson, Book.class);

        // Remove "Bearer " prefix from token
        token = token.replace("Bearer ", "");

        // Extract role from token
        String role = jwtUtil.extractRole(token);
        System.out.println("User Role: " + role);

        // Only allow ADMIN to add a book
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Only ADMIN users can add books.");
        }

        // Handle file upload
        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                imagePath = imageUploader.uploadFile(file);
            } catch (IOException e) {
                throw new RuntimeException("File upload failed: " + e.getMessage());
            }
        }

        // Save the book record with the image path
        book.setBookCoverPath(imagePath);
        int result = bookService.addBook(book);
        if (result > 0) {
            return ResponseEntity.ok("Book added successfully!");
        } else {
            throw new DataAccessException("Failed to add book.") {};
        }
    }

    @DeleteMapping("/delete/{bookId}")
    public ResponseEntity<String> deleteBook(
            @RequestHeader("Authorization") String token,
            @PathVariable int bookId
    ) {
        token = token.replace("Bearer ", "");
        String role = jwtUtil.extractRole(token);

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Only ADMIN users can delete books.");
        }

        boolean deleted = bookService.deleteBook(bookId);
        if (deleted) {
            return ResponseEntity.ok("Book deleted successfully.");
        } else {
            return ResponseEntity.badRequest().body("Book not found or could not be deleted.");
        }
    }

    @GetMapping("/searchCache")
    public ResponseEntity<Map<String, Object>> searchBooksFromCache(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sortByPrice
    ) {
        Map<String, Object> data = bookService.getFilterPaginatedBooksFromCache(page, size, category, author, genre, title, sortByPrice);

        List<Book> books = (List<Book>) data.get("results");

        books.forEach(book -> {
            if (book.getBookCoverPath() != null && !book.getBookCoverPath().startsWith("http")) {
                book.setBookCoverPath("http://localhost:8080/bookCovers/" + book.getBookCoverPath());
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("results", books);
        response.put("totalCount", data.get("totalCount"));
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(@ModelAttribute SearchAndFilter filter) {
        Map<String, Object> result = bookService.searchAndPaginateBooks(filter);
        System.out.println("before filtering"+result);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getBooksFromCachePaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Book> books = bookService.getPaginatedBooksFromCache(page, size);

        // Prepend full image URL
        books.forEach(book -> {
            if (book.getBookCoverPath() != null && !book.getBookCoverPath().startsWith("http")) {
                book.setBookCoverPath("http://localhost:8080/bookCovers/" + book.getBookCoverPath());
            }
        });

        int totalBooks = bookService.getTotalBookCount(); // from DB

        Map<String, Object> response = new HashMap<>();
        response.put("books", books);
        response.put("totalCount", totalBooks);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateBook(
            @RequestHeader("Authorization") String token,
            @RequestPart("book") String bookJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Book book = objectMapper.readValue(bookJson, Book.class);

        token = token.replace("Bearer ", "");
        String role = jwtUtil.extractRole(token);

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Only ADMIN users can update books.");
        }

        String imagePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                imagePath = imageUploader.uploadFile(file);
                book.setBookCoverPath(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("File upload failed: " + e.getMessage());
            }
        }

        boolean updated = bookService.updateBook(book);
        if (updated) {
            return ResponseEntity.ok("Book updated successfully!");
        } else {
            return ResponseEntity.badRequest().body("Book update failed.");
        }
    }




}