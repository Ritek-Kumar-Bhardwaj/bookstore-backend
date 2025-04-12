package com.bookstore.api.services;

import com.bookstore.api.dto.Category;
import com.bookstore.api.dto.Genre;
import com.bookstore.api.dto.SearchAndFilter;
import com.bookstore.api.models.Book;
import com.bookstore.api.repositories.BookRepository;
import com.bookstore.api.repositories.BookRepositoryInterface;
import com.bookstore.api.security.XssSanitizer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

//    @Autowired
//    private BookRepositoryInterface bookRepository;

    private final Map<Integer, Book> bookCache = new LinkedHashMap<>();
    private final Map<Integer, String> coverImageCache = new LinkedHashMap<>();


    private static final Map<String, String> genreToCategoryMap = Map.ofEntries(
            Map.entry("Fantasy", "Fiction"), Map.entry("Science Fiction", "Fiction"),
            Map.entry("Mystery & Thriller", "Fiction"), Map.entry("Horror", "Fiction"),
            Map.entry("Romance", "Fiction"), Map.entry("Historical Fiction", "Fiction"),
            Map.entry("Adventure", "Fiction"), Map.entry("Contemporary Fiction", "Fiction"),
            Map.entry("Biographies & Memoirs", "Non-Fiction"), Map.entry("Self-Help & Personal Development", "Non-Fiction"),
            Map.entry("Health & Wellness", "Non-Fiction"), Map.entry("Business & Economics", "Non-Fiction"),
            Map.entry("Psychology", "Non-Fiction"), Map.entry("History", "Non-Fiction"),
            Map.entry("Science & Technology", "Non-Fiction"), Map.entry("True Crime", "Non-Fiction"),
            Map.entry("Textbooks", "Educational & Academic"), Map.entry("Research Papers & Journals", "Educational & Academic"),
            Map.entry("Law & Legal Studies", "Educational & Academic"), Map.entry("Medical & Healthcare", "Educational & Academic"),
            Map.entry("Engineering & Technology", "Educational & Academic"), Map.entry("Computer Science & Programming", "Educational & Academic"),
            Map.entry("Mathematics", "Educational & Academic"), Map.entry("Language & Linguistics", "Educational & Academic"),
            Map.entry("Photography", "Arts & Lifestyle"), Map.entry("Music & Performing Arts", "Arts & Lifestyle"),
            Map.entry("Travel & Tourism", "Arts & Lifestyle"), Map.entry("Cooking & Food", "Arts & Lifestyle"),
            Map.entry("Fashion & Design", "Arts & Lifestyle"), Map.entry("Home & Garden", "Arts & Lifestyle"),
            Map.entry("Christianity", "Religious & Spiritual"), Map.entry("Islam", "Religious & Spiritual"),
            Map.entry("Hinduism", "Religious & Spiritual"), Map.entry("Buddhism", "Religious & Spiritual"),
            Map.entry("Meditation & Mindfulness", "Religious & Spiritual"), Map.entry("Philosophy", "Religious & Spiritual"),
            Map.entry("Picture Books", "Children & Young Adult"), Map.entry("Fairy Tales & Folklore", "Children & Young Adult"),
            Map.entry("Young Adult Fiction", "Children & Young Adult"), Map.entry("Comics & Graphic Novels", "Children & Young Adult")
    );

    public List<Book> getPaginatedBooksFromCache(int page, int size) {
        List<Book> allBooks;

        allBooks = new ArrayList<>(bookCache.values());

        int start = page * size;
        int end = Math.min(start + size, allBooks.size());

        if (start >= allBooks.size()) return Collections.emptyList();

        return allBooks.subList(start, end);
    }

    public Map<String, Object> getFilterPaginatedBooksFromCache(
            int page,
            int size,
            String category,
            String author,
            String genre,
            String title,
            String sortByPrice
    ) {
        List<Book> allBooks = new ArrayList<>(bookCache.values());

        // Step 1: Apply filters
        Stream<Book> filteredStream = allBooks.stream()
                .filter(book -> {
                    if (title != null && !title.isEmpty()) {
                        if (book.getTitle() == null || !book.getTitle().toLowerCase().contains(title.toLowerCase())) {
                            return false;
                        }
                    }

                    if (author != null && !author.isEmpty()) {
                        if (book.getAuthor() == null || !book.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                            return false;
                        }
                    }

                    if (genre != null && !genre.isEmpty()) {
                        if (book.getGenre() == null ||
                                Arrays.stream(book.getGenre().split(","))
                                        .map(String::trim)
                                        .noneMatch(g -> g.equalsIgnoreCase(genre))) {
                            return false;
                        }
                    }

                    if (category != null && !category.isEmpty()) {
                        if (book.getGenre() == null) return false;

                        boolean matchesCategory = Arrays.stream(book.getGenre().split(","))
                                .map(String::trim)
                                .map(g -> genreToCategoryMap.getOrDefault(g, ""))
                                .anyMatch(c -> c.equalsIgnoreCase(category));

                        if (!matchesCategory) return false;
                    }

                    return true;
                });

        // Optional: Sort
        if ("asc".equalsIgnoreCase(sortByPrice)) {
            filteredStream = filteredStream.sorted(Comparator.comparingDouble(Book::getPrice));
        } else if ("desc".equalsIgnoreCase(sortByPrice)) {
            filteredStream = filteredStream.sorted(Comparator.comparingDouble(Book::getPrice).reversed());
        }

        List<Book> filteredBooks = filteredStream.toList();

        // Step 2: Pagination
        int totalCount = filteredBooks.size();
        int start = page * size;
        int end = Math.min(start + size, totalCount);

        List<Book> paginatedBooks = (start < totalCount) ? filteredBooks.subList(start, end) : Collections.emptyList();

        // Return both total and result
        Map<String, Object> result = new HashMap<>();
        result.put("results", paginatedBooks);
        result.put("totalCount", totalCount);
        return result;
    }




    public Map<String, Object> searchAndPaginateBooks(SearchAndFilter filter) {
        List<Book> books = bookRepository.searchBooks(filter);
        int total = bookRepository.getFilteredBookCount(filter);

        for (Book book : books) {
            String genres = bookRepository.getGenresByBookId(book.getBookId());
            book.setGenre(genres);
        }

        List<Category> categories = bookRepository.findAllCategorys();
        List<Genre> genres = bookRepository.findAllGenres();

        Map<String, Object> response = new HashMap<>();
        response.put("result", books);
        response.put("totalCount", total);
        response.put("categories", categories);
        response.put("genres", genres);

        return response;
    }


    public void printAllBooksFromCache() {
        if (bookCache.isEmpty()) {
            System.out.println("Book cache is empty.");
            return;
        }

        for (Map.Entry<Integer, Book> entry : bookCache.entrySet()) {
            int bookId = entry.getKey();
            Book book = entry.getValue();
            System.out.println("Book ID: " + bookId);
            System.out.println("Title: " + book.getTitle());
            System.out.println("Author: " + book.getAuthor());
            System.out.println("Genre: " + book.getGenre());
            System.out.println("Price: ₹" + book.getPrice());
            System.out.println("Stock: " + book.getStockQuantity());
            System.out.println("Cover Path: " + book.getBookCoverPath());
            System.out.println("-------------------------------");
        }
    }

    @PostConstruct
    public void initializeCache() {
        List<Book> books = bookRepository.findAllBooks();

        for (Book book : books) {
            bookCache.put(book.getBookId(), book);
            coverImageCache.put(book.getBookId(), book.getBookCoverPath());
        }

        System.out.println("Book cache initialized with " + bookCache.size() + " books");
        System.out.println("Cover image cache initialized with " + coverImageCache.size() + " entries");

        printAllBooksFromCache();
    }

    public Map<Integer, Book> getBookCache() {
        return bookCache;
    }

    public Map<Integer, String> getCoverImageCache() {
        return coverImageCache;
    }

    public String getCoverImageByBookId(int bookId) {
        return coverImageCache.get(bookId);
    }

    public void refreshBookCache() {
        bookCache.clear();
        coverImageCache.clear();
        initializeCache();
    }

    public int addBook(Book book) {
        // 1. Sanitize input
        book.setTitle(XssSanitizer.sanitizeName(book.getTitle()));
        book.setAuthor(XssSanitizer.sanitizeName(book.getAuthor()));
        book.setDescription(XssSanitizer.sanitizeAddress(book.getDescription()));
        book.setGenre(XssSanitizer.sanitizeAddress(book.getGenre()));

        // 2. Save book and get generated ID
        int bookId = bookRepository.save(book);

        // 3. Add to cache only if book was saved successfully
        if (bookId > 0) {
            book.setBookId(bookId);
            synchronized (bookCache) {
                bookCache.put(bookId, book);
                coverImageCache.put(bookId, book.getBookCoverPath());
            }
            System.out.println("Book added to cache: " + bookCache.get(bookId));
        } else {
            System.out.println("Failed to add book to DB. Not caching.");
        }

        return bookId;
    }

    public boolean deleteBook(int bookId) {
        Book removedBook;

        synchronized (bookCache) {
            removedBook = bookCache.remove(bookId);
            coverImageCache.remove(bookId);
        }

        if (removedBook != null) {
            System.out.println("Book removed from cache: " + removedBook);
            return bookRepository.deleteBook(bookId);
        } else {
            System.out.println("Book not found in cache. Skipping DB delete.");
            return false;
        }
    }


//    public List<Book> searchBooks(SearchAndFilter filter) {
//        return bookRepository.searchBooks(filter);
//    }

    public int getTotalBookCount() {
        return bookRepository.getTotalBookCount();
    }

//    public List<Book> getPaginatedBooksFromCache(int page, int size) {
//        List<Book> allBooks;
//
//        allBooks = new ArrayList<>(bookCache.values());
//
//        int start = page * size;
//        int end = Math.min(start + size, allBooks.size());
//
//        if (start >= allBooks.size()) return Collections.emptyList();
//
//        return allBooks.subList(start, end);
//    }

    public boolean updateBook(Book book) {
        // Sanitize input
        book.setTitle(XssSanitizer.sanitizeName(book.getTitle()));
        book.setAuthor(XssSanitizer.sanitizeName(book.getAuthor()));
        book.setDescription(XssSanitizer.sanitizeAddress(book.getDescription()));
        book.setGenre(XssSanitizer.sanitizeAddress(book.getGenre()));

        boolean updated = bookRepository.updateBook(book);

        if (updated) {
            synchronized (bookCache) {
                bookCache.put(book.getBookId(), book);
                coverImageCache.put(book.getBookId(), book.getBookCoverPath());
            }
            System.out.println("Book updated in cache: " + book);
        }

        return updated;
    }


}
