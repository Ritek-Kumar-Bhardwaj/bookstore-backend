package com.bookstore.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "EVgvqCXHPNP/8D54hCiUulMyHtKhanQb5zi8xsLKyxE="; // 256-bit Base64 encoded

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Map<String, Object> generateToken(String email, String role) {
    	long currentTimeMillis = System.currentTimeMillis();
        Date issuedAt = new Date(currentTimeMillis);
        Date expiration = new Date(currentTimeMillis + 1000 * 60 * 60); // 1 hour

        String token = Jwts.builder()
        		.setSubject(email)  // ✅ Correct method
                .claim("role", role)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration) // 1 hour
                .signWith(getSigningKey()) // Secure key
                .compact();

        return Map.of(
            "token", token,
            "issued_at", issuedAt.getTime(),
            "expires_at", expiration.getTime()
        );
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder() // ✅ Corrected method for JJWT 0.11+
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, String email) {
        return (email.equals(extractEmail(token))) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
