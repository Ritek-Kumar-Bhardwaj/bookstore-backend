package com.bookstore.api.repositories;

import com.bookstore.api.models.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CartRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Integer findLatestCartIdByUserId(int userId) {
        System.out.println("9");
        System.out.println("userid - "+userId);
        String sql = "SELECT cart_id FROM carts WHERE user_id = ? ORDER BY created_at DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String getCartStatus(int cartId) {
        System.out.println("8");
        String sql = "SELECT status FROM carts WHERE cart_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, cartId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

//    public void createCart(int userId) {
//        String sql = "INSERT INTO carts (user_id, status, total_amount) VALUES (?, 'Active', 0.00)";
//        jdbcTemplate.update(sql, userId);
//    }

    public int createCart(int userId) {
        System.out.println("7");
        String sql = "INSERT INTO carts (user_id, status, total_amount) " +
                "VALUES (?, 'Active', 0.00)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();  // returns the new cart_id
    }


    public List<CartItem> getCartItemsByCartId(int cartId) {
        System.out.println("6");
        String sql = "SELECT cart_item_id, book_id, quantity, price, added_at FROM cart_items WHERE cart_id = ?";

        return jdbcTemplate.query(sql, new Object[]{cartId}, (rs, rowNum) -> {
            CartItem item = new CartItem();
            item.setCartItemId(rs.getInt("cart_item_id"));
            item.setBookId(rs.getInt("book_id"));
            item.setQuantity(rs.getInt("quantity"));
            item.setPrice(rs.getDouble("price"));
            item.setAddedAt(rs.getTimestamp("added_at"));
            return item;
        });
    }

//    public void save(CartItem item) {
//        if (item.getCartItemId() != 0) {
//            // Existing item: update
//            System.out.println("1");
//            String updateSql = "UPDATE cart_items SET quantity = ?, price = ? WHERE cart_item_id = ?";
//            jdbcTemplate.update(updateSql, item.getQuantity(), item.getPrice(), item.getCartItemId());
//        } else {
//            // New item: insert
//            String insertSql = "INSERT INTO cart_items (cart_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";
//            jdbcTemplate.update(insertSql, item.getCartId(), item.getBookId(), item.getQuantity(), item.getPrice());
//        }
//    }
public void save(CartItem item) {
    Optional<CartItem> existing = findByCartIdAndBookId(item.getCartId(), item.getBookId());

    if (existing.isPresent()) {
        // Update quantity and price
        CartItem existingItem = existing.get();
        System.out.println("Updating existing item");
        String updateSql = "UPDATE cart_items SET quantity = ?, price = ? WHERE cart_item_id = ?";
        jdbcTemplate.update(updateSql, item.getQuantity(), item.getPrice(), existingItem.getCartItemId());
    } else {
        // Insert new
        System.out.println("Inserting new item");
        String insertSql = "INSERT INTO cart_items (cart_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, item.getCartId(), item.getBookId(), item.getQuantity(), item.getPrice());
    }
}



    public Optional<CartItem> findByCartIdAndBookId(int cartId, int bookId) {
        String sql = "SELECT cart_item_id, book_id, quantity, price, added_at " +
                "FROM cart_items WHERE cart_id = ? AND book_id = ?";
        try {
            System.out.println("2");
            CartItem item = jdbcTemplate.queryForObject(sql, new Object[]{cartId, bookId}, (rs, rowNum) -> {
                CartItem ci = new CartItem();
                ci.setCartItemId(rs.getInt("cart_item_id"));
                ci.setBookId(rs.getInt("book_id"));
                ci.setQuantity(rs.getInt("quantity"));
                ci.setPrice(rs.getDouble("price"));
                ci.setAddedAt(rs.getTimestamp("added_at"));
                ci.setCartId(cartId);
                return ci;
            });
            return Optional.ofNullable(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public void updateCartTotalAmount(int cartId) {
        System.out.println("3");
        String sql = "UPDATE carts SET total_amount = (" +
                "SELECT IFNULL(SUM(price * quantity), 0.00) FROM cart_items WHERE cart_id = ?" +
                ") WHERE cart_id = ?";
        jdbcTemplate.update(sql, cartId, cartId);
    }



    public boolean addItemToCart(int cartId, int bookId, double price) {
        System.out.println("4");
        String sql = "INSERT INTO cart_items (cart_id, book_id, quantity, price) VALUES (?, ?, 1, ?)";
        return jdbcTemplate.update(sql, cartId, bookId, price) > 0;
    }

    public void deleteAllItemsFromCart(int cartId) {
        System.out.println("5");
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        jdbcTemplate.update(sql, cartId);
    }




}
