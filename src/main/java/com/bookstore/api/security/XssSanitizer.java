package com.bookstore.api.security;

import org.owasp.html.HtmlPolicyBuilder;

import org.owasp.html.PolicyFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class XssSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .toFactory(); // Strict policy to remove HTML elements
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public static String sanitizeName(String input) {
        if (input == null) return null;
        String sanitized = POLICY.sanitize(input);
        return sanitized.replaceAll("[^a-zA-Z\\s]", ""); // Allow only letters and spaces
    }

    public static String sanitizeContactNumber(String input) {
        if (input == null) return null;
        String sanitized = POLICY.sanitize(input);
        return sanitized.replaceAll("[^0-9]", ""); // Allow only numbers
    }

    public static String sanitizeAddress(String input) {
        if (input == null) return null;
        String sanitized = POLICY.sanitize(input);
        return input.replaceAll("[^a-zA-Z, &-]", ""); // Allow letters, spaces, and commas
    }

    public static String sanitizeEmail(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9@.]", ""); // Allow letters, numbers, @, and .
    }

    public static String sanitizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        // Sanitize input to remove unwanted characters
        return POLICY.sanitize(imageUrl);
    }

    public static String hashPassword(String password) {
        if (password == null) return null;
        return PASSWORD_ENCODER.encode(password); // Hash password securely
    }
}
