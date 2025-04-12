package com.bookstore.api.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class PaymentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Fetch payment details by order ID
    public Map<String, Object> getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ?";
        return jdbcTemplate.queryForMap(sql, orderId);
    }

    // Update the payment status after successful payment
    public void updatePaymentStatus(int orderId, String paymentStatus) {
        String sql = "UPDATE payments SET payment_status = ? WHERE order_id = ?";
        jdbcTemplate.update(sql, paymentStatus, orderId);
    }
}
