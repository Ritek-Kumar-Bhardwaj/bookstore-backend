package com.bookstore.api.repositories;

import com.bookstore.api.models.OrderItem;
import com.bookstore.api.dto.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Repository
public class OrderRepository {

//    private final JdbcTemplate jdbc;
//
//    public OrderRepository(JdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> insertOrder(OrderRequest request) {
        // Generate a random 6-digit code
        String deliveryCode = String.format("%06d", (int)(Math.random() * 1000000));

        // Insert into orders table
        String insertOrderSQL = "INSERT INTO orders (user_id, total_amount, order_date, status, payment_method, delivery_code) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertOrderSQL, request.userId(), request.totalAmount(), new Timestamp(System.currentTimeMillis()),
                "Pending", request.paymentMethod(), deliveryCode);

        Integer orderId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        for (OrderItem item : request.items()) {
            String insertItemSQL = "INSERT INTO order_items (order_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertItemSQL, orderId, item.bookId(), item.quantity(), item.price());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("deliveryCode", deliveryCode);
        response.put("message", "Order placed successfully.");
        return response;
    }

    // Fetch order details by order ID
    public Map<String, Object> getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        return jdbcTemplate.queryForMap(sql, orderId);
    }

    // Update the order status after payment confirmation
    public void updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        jdbcTemplate.update(sql, newStatus, orderId);
    }


}

