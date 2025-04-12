package com.bookstore.api.controllers;

import com.bookstore.api.dto.LoginRequest;

import com.bookstore.api.dto.LoginResponse;
import com.bookstore.api.dto.UpdateUserRequest;
import com.bookstore.api.models.User;
import com.bookstore.api.security.JwtUtil;
import com.bookstore.api.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.bookstore.api.security.XssSanitizer;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        User alreadyExist = userService.getUserByEmail(XssSanitizer.sanitizeEmail(user.getEmail()));
        if (alreadyExist != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        }


        try {
            userService.registerUser(user);
            return ResponseEntity.ok(Map.of("message", "User registered successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

//    @PostMapping("/login")
//    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
//        if (bindingResult.hasErrors()) {
//            StringBuilder errorMessage = new StringBuilder();
//            for (FieldError error : bindingResult.getFieldErrors()) {
//                errorMessage.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
//            }
//            return ResponseEntity.badRequest().body(errorMessage.toString().trim());
//        }
//
//        // Authenticate user and get role
//        String role = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
//
//        if (role != null) {
//            // Generate JWT token
//        	 Map<String, Object> tokenData = jwtUtil.generateToken(loginRequest.getEmail(), role);
//
//             return ResponseEntity.ok(Map.of(
//                 "message", "Login successful.",
//                 "role", role,
//                 "token", tokenData.get("token"),
//                 "issued_at", tokenData.get("issued_at"),
//                 "expires_at", tokenData.get("expires_at")
//             ));
//        } else {
//            return ResponseEntity.badRequest().body("Invalid email or password.");
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMessage.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return ResponseEntity.badRequest().body(errorMessage.toString().trim());
        }

        LoginResponse loginResponse = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

        if (loginResponse != null) {
            Map<String, Object> tokenData = jwtUtil.generateToken(loginRequest.getEmail(), loginResponse.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful.");
            response.put("role", loginResponse.getRole());
            response.put("token", tokenData.get("token"));
            response.put("issued_at", tokenData.get("issued_at"));
            response.put("expires_at", tokenData.get("expires_at"));
            response.put("user_id", loginResponse.getUserId()); // Include user ID

            if ("CUSTOMER".equals(loginResponse.getRole())) {
                response.put("cart_id", loginResponse.getCartId());
                response.put("cart_items", loginResponse.getCartItems());
            }

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body("Invalid email or password.");
        }
    }



    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix from token
        token = token.replace("Bearer ", "");

        // Extract email from token
        String email = jwtUtil.extractEmail(token);

        // Fetch user details from database
        User user = userService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(Map.of(
            "email", user.getEmail(),
            "name", user.getName(),
            "contact_number", user.getContactNumber(),
            "address", user.getAddress(),
            "role", jwtUtil.extractRole(token)
        ));
    }
    
    @PutMapping("/updateuser")
    public ResponseEntity<?> updateUser(
            @RequestHeader("Authorization") String authHeader, // Extract token
            @Valid @RequestBody UpdateUserRequest updatedUser,
            BindingResult bindingResult) {
        // ✅ Validate request
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMessage.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return ResponseEntity.badRequest().body(errorMessage.toString().trim());
        }

        // ✅ Extract token & email from the Authorization header
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token); // Get email from JWT

        // ✅ Update user details in DB
        int updatedUserData = userService.updateUser(email, updatedUser);
        if (updatedUserData == 0) {
            return ResponseEntity.badRequest().body("User not found or update failed.");
        }

        return ResponseEntity.ok(Map.of(
                "message", "User updated successfully."
        ));
    }
}
