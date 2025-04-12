package com.bookstore.api.controllers;

import com.bookstore.api.services.ForgotPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }

        boolean isProcessed = forgotPasswordService.processForgotPassword(email);

        if (!isProcessed) {
            return ResponseEntity.badRequest().body("Email not found.");
        }

        return ResponseEntity.ok("A temporary password has been sent to your email.");
    }
}
