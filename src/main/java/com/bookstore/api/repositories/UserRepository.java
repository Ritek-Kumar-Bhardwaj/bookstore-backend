package com.bookstore.api.repositories;

import com.bookstore.api.models.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public int save(User user) {
        String userSql = "INSERT INTO users (name, email, password, contact_number, address) VALUES (?, ?, ?, ?, ?)";
        String cartSql = "INSERT INTO carts (user_id, status, total_amount) VALUES (?, 'Active', 0.00)";

        return jdbcTemplate.execute(connection -> {
            PreparedStatement ps = connection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getContactNumber());
            ps.setString(5, user.getAddress());
            return ps;
        }, (PreparedStatementCallback<Integer>) ps -> {
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);

                        // Insert a cart for this user
                        jdbcTemplate.update(cartSql, userId);
                    }
                }
            }

            return rowsAffected; // Only return rows affected for the user insert
        });
    }


    public int update(User user, String email) {
        String sql = "UPDATE users SET name = ?, password = ?, contact_number = ?, address = ?, email = ? WHERE email = ?";

        int rowsAffected = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getName());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getContactNumber());
            ps.setString(4, user.getAddress());
            ps.setString(5, user.getEmail());
            ps.setString(6, email); // Identify the user by the original email
            return ps;
        });

        return rowsAffected;
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return jdbcTemplate.query(sql, new UserRowMapper(), email)
                .stream().findFirst();
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setContactNumber(rs.getString("contact_number"));
            user.setAddress(rs.getString("address"));
            return user;
        }
    }
    
    public int updatePassword(String email, String hashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        return jdbcTemplate.update(sql, hashedPassword, email);
    }
}
