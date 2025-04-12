package com.bookstore.api.controllers;

import com.bookstore.api.services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Endpoint to initiate a payment request (for testing purposes)
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable int orderId) {
        Map<String, String> response = paymentService.initiatePayment(orderId);
        return ResponseEntity.ok(response);
    }

    // Endpoint to confirm payment status after transaction (for testing purposes)
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(@RequestParam("orderId") int orderId) {
        paymentService.verifyAndUpdatePayment(orderId);
        Map<String, String> response = Map.of("message", "Payment confirmed and order status updated.");
        return ResponseEntity.ok(response);
    }
}

