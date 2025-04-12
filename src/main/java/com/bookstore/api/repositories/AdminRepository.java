package com.bookstore.api.repositories;

import com.bookstore.api.models.Admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class AdminRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public Optional<Admin> findByEmail(String email) {
        String sql = "SELECT * FROM admin WHERE email = ?";
        return jdbcTemplate.query(sql, new AdminRowMapper(), email)
                .stream().findFirst();
    }

    private static class AdminRowMapper implements RowMapper<Admin> {
        @Override
        public Admin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Admin admin = new Admin();
            admin.setUsername(rs.getString("username"));
            admin.setEmail(rs.getString("email"));
            admin.setPassword(rs.getString("password"));
            return admin;
        }
    }
}
