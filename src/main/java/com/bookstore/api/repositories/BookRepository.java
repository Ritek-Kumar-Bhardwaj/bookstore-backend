package com.bookstore.api.repositories;

import com.bookstore.api.dto.Category;
import com.bookstore.api.dto.Genre;
import com.bookstore.api.dto.SearchAndFilter;
import com.bookstore.api.models.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Repository
public class BookRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int save(Book book) {
        String sql = "INSERT INTO books (title, author, price, stock_quantity, description, genre) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setDouble(3, book.getPrice());
            ps.setInt(4, book.getStockQuantity());
            ps.setString(5, book.getDescription());
            ps.setString(6, book.getGenre());
            return ps;
        }, keyHolder);

        if (rowsAffected > 0) {
            int bookId = keyHolder.getKey().intValue();
            updateBookGenres(bookId, book.getGenre());
            uploadBookCover(bookId,book.getBookCoverPath());
            return bookId;
        }
        return 0;
    }

    private void updateBookGenres(int bookId, String genreText) {
        if (genreText == null || genreText.isEmpty()) return;

        // Convert plain text genres into a list
        System.out.println(genreText);
        List<String> genreNames = Arrays.stream(genreText.split(","))
                .map(String::trim) // Trim spaces around each genre
                .collect(Collectors.toList());

        for (String genreName : genreNames) {
            Integer genreId = findGenreIdByName(genreName.trim());
            if (genreId != null) {
                String sql = "INSERT INTO book_genres (book_id, genre_id) VALUES (?, ?)";
                jdbcTemplate.update(sql, bookId, genreId);
            }
        }
    }

    private Integer findGenreIdByName(String genreName) {
        String sql = "SELECT genre_id FROM genres WHERE name = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, genreName);
        } catch (EmptyResultDataAccessException e) {
            return null; // Genre not found
        }
    }

    public boolean existsById(int bookId) {
        String sql = "SELECT COUNT(*) FROM books WHERE book_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, bookId);
        return count != null && count > 0;
    }

    private void uploadBookCover(int bookId, String bookCoverPath) {
        String sql = "INSERT INTO book_images (book_id, image_url) VALUES (?, ?)";
        jdbcTemplate.update(sql, bookId, bookCoverPath);
    }

    public void deleteById(int bookId) {
        String sql = "DELETE FROM books WHERE book_id = ?";
        jdbcTemplate.update(sql, bookId);
    }

    // RowMapper to map result set to Book object
    private final RowMapper<Book> bookRowMapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setPrice(rs.getDouble("price"));
        book.setStockQuantity(rs.getInt("stock_quantity"));
        book.setDescription(rs.getString("description"));
        book.setGenre(rs.getString("genre"));
//        book.setBookCoverPath(rs.getString(getBookCoverPath(rs.getInt("book_id"))));
        return book;
    };

    // Fetch books from database
    public List<Book> findAllBooks() {
        String sql = "SELECT * FROM books ORDER BY book_id DESC";
        List<Book> books = jdbcTemplate.query(sql, bookRowMapper);

        if (!books.isEmpty()) {
            Map<Integer, String> bookCoverPaths = getBookCoverPaths(books);
            for (Book book : books) {
                book.setBookCoverPath(bookCoverPaths.get(book.getBookId()));
            }
        }

        return books;
    }

    // Batch fetch book cover paths
    public Map<Integer, String> getBookCoverPaths(List<Book> books) {
        Map<Integer, String> coverPaths = new HashMap<>();

        if (books.isEmpty()) return coverPaths; // Return empty if no books

        // Generate placeholders for SQL IN clause
        String placeholders = books.stream().map(b -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT book_id, image_url FROM book_images WHERE book_id IN (" + placeholders + ")";


        List<Object> bookIds = books.stream().map(Book::getBookId).collect(Collectors.toList());

        jdbcTemplate.query(sql, bookIds.toArray(), (rs) -> {
            coverPaths.put(rs.getInt("book_id"), rs.getString("image_url"));
        });

        return coverPaths;
    }

    public List<Book> searchBooks(SearchAndFilter filter) {
        StringBuilder sql = new StringBuilder("""
    SELECT 
        b.book_id,
        ANY_VALUE(b.title) AS title,
        ANY_VALUE(b.author) AS author,
        ANY_VALUE(b.price) AS price,
        ANY_VALUE(b.stock_quantity) AS stock_quantity,
        ANY_VALUE(b.description) AS description,
        MIN(bi.image_url) AS image_url,
        GROUP_CONCAT(DISTINCT g.name ORDER BY g.name ASC SEPARATOR ', ') AS genres,
        ANY_VALUE(c.name) AS category
    FROM books b
    LEFT JOIN book_images bi ON b.book_id = bi.book_id
    LEFT JOIN book_genres bg ON b.book_id = bg.book_id
    LEFT JOIN genres g ON g.genre_id = bg.genre_id
    LEFT JOIN categories c ON g.category_id = c.category_id
    WHERE 1 = 1
""");

        List<Object> params = new ArrayList<>();

        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            sql.append(" AND LOWER(b.title) LIKE LOWER(?)");
            params.add("%" + filter.getTitle() + "%");
        }

        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            sql.append(" AND LOWER(b.author) LIKE LOWER(?)");
            params.add("%" + filter.getAuthor() + "%");
        }

        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            sql.append(" AND LOWER(c.name) = LOWER(?)");
            params.add(filter.getCategory());
        }

        if (filter.getGenre() != null && !filter.getGenre().isBlank()) {
            sql.append("""
        AND EXISTS (
            SELECT 1
            FROM book_genres bg2
            JOIN genres g2 ON bg2.genre_id = g2.genre_id
            WHERE bg2.book_id = b.book_id AND LOWER(g2.name) = LOWER(?)
        )
    """);
            params.add(filter.getGenre());
        }

        if (filter.getMaxPrice() != null) {
            sql.append(" AND b.price <= ?");
            params.add(filter.getMaxPrice());
        }

        if (filter.getStock() != null && filter.getStock()) {
            sql.append(" AND b.stock_quantity > 0");
        }

        // GROUP BY
        sql.append(" GROUP BY b.book_id");

        // Sorting logic
        // Sorting logic
        List<String> orderClauses = new ArrayList<>();

        if (filter.getSortByPrice() != null) {
            if (filter.getSortByPrice().equalsIgnoreCase("asc")) {
                orderClauses.add("b.price ASC");
            } else if (filter.getSortByPrice().equalsIgnoreCase("desc")) {
                orderClauses.add("b.price DESC");
            }
        }

        if (filter.getSortByStock() != null) {
            if (filter.getSortByStock().equalsIgnoreCase("asc")) {
                orderClauses.add("b.stock_quantity ASC");
            } else if (filter.getSortByStock().equalsIgnoreCase("desc")) {
                orderClauses.add("b.stock_quantity DESC");
            }
        }

        if (orderClauses.isEmpty()) {
            orderClauses.add("b.book_id DESC"); // default sorting
        }

        sql.append(" ORDER BY ").append(String.join(", ", orderClauses));


        // Pagination
        sql.append(" LIMIT ? OFFSET ?");
        params.add(filter.getSize());
        params.add(filter.getPage() * filter.getSize());

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Book book = new Book();
            book.setBookId(rs.getInt("book_id"));
            book.setTitle(rs.getString("title"));
            book.setAuthor(rs.getString("author"));
            book.setPrice(rs.getDouble("price"));
            book.setStockQuantity(rs.getInt("stock_quantity"));
            book.setDescription(rs.getString("description"));
            book.setGenre(rs.getString("genres"));
            book.setBookCoverPath(rs.getString("image_url"));
            System.out.println(book.getBookId());
            return book;
        });
    }


    public int getFilteredBookCount(SearchAndFilter filter) {
        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT b.book_id)
        FROM books b
        LEFT JOIN book_genres bg ON b.book_id = bg.book_id
        LEFT JOIN genres g ON g.genre_id = bg.genre_id
        LEFT JOIN categories c ON g.category_id = c.category_id
        WHERE 1 = 1
    """);

        List<Object> params = new ArrayList<>();

        if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
            sql.append(" AND LOWER(b.title) LIKE LOWER(?)");
            params.add("%" + filter.getTitle() + "%");
        }

        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            sql.append(" AND LOWER(b.author) LIKE LOWER(?)");
            params.add("%" + filter.getAuthor() + "%");
        }

        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            sql.append(" AND LOWER(c.name) = LOWER(?)");
            params.add(filter.getCategory());
        }

        if (filter.getGenre() != null && !filter.getGenre().isBlank()) {
            sql.append("""
        AND EXISTS (
            SELECT 1
            FROM book_genres bg2
            JOIN genres g2 ON bg2.genre_id = g2.genre_id
            WHERE bg2.book_id = b.book_id AND LOWER(g2.name) = LOWER(?)
        )
    """);
            params.add(filter.getGenre());
        }

        if (filter.getMaxPrice() != null) {
            sql.append(" AND b.price <= ?");
            params.add(filter.getMaxPrice());
        }

        if (filter.getStock() != null && filter.getStock()) {
            sql.append(" AND b.stock_quantity > 0");
        }

        return jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Integer.class);
    }

    public String getGenresByBookId(int bookId) {
        String sql = """
        SELECT GROUP_CONCAT(g.name ORDER BY g.name ASC SEPARATOR ', ')
        FROM book_genres bg
        JOIN genres g ON bg.genre_id = g.genre_id
        WHERE bg.book_id = ?
    """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class, bookId);
        } catch (EmptyResultDataAccessException e) {
            return ""; // Return empty if no genres found
        }
    }



    public int getTotalBookCount() {
        String sql = "SELECT COUNT(*) FROM books";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, price = ?, stock_quantity = ?, description = ?, genre = ? WHERE book_id = ?";
        int rows = jdbcTemplate.update(sql, book.getTitle(), book.getAuthor(), book.getPrice(),
                book.getStockQuantity(), book.getDescription(), book.getGenre(), book.getBookId());

        // Update genres
        jdbcTemplate.update("DELETE FROM book_genres WHERE book_id = ?", book.getBookId());
        updateBookGenres(book.getBookId(), book.getGenre());

        // Update image path if present
        if (book.getBookCoverPath() != null) {
            jdbcTemplate.update("DELETE FROM book_images WHERE book_id = ?", book.getBookId());
            uploadBookCover(book.getBookId(), book.getBookCoverPath());
        }

        return rows > 0;
    }

    public boolean deleteBook(int bookId) {
        jdbcTemplate.update("DELETE FROM book_genres WHERE book_id = ?", bookId);
        jdbcTemplate.update("DELETE FROM book_images WHERE book_id = ?", bookId);
        String sql = "DELETE FROM books WHERE book_id = ?";
        int rows = jdbcTemplate.update(sql, bookId);
        return rows > 0;
    }

    public List<Category> findAllCategorys() {
        String sql = "SELECT * FROM categories";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Category c = new Category();
            c.setCategoryId(rs.getInt("category_id"));
            c.setName(rs.getString("name"));
            c.setDescription(rs.getString("description"));
            return c;
        });
    }

    public List<Genre> findAllGenres() {
        String sql = "SELECT * FROM genres";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre g = new Genre();
            g.setGenreId(rs.getInt("genre_id"));
            g.setName(rs.getString("name"));
            g.setDescription(rs.getString("description"));
            g.setCategoryId(rs.getInt("category_id"));
            return g;
        });
    }


}
