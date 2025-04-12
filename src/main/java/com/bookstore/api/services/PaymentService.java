package com.bookstore.api.services;

import com.bookstore.api.repositories.OrderRepository;
import com.bookstore.api.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${cashfree.client.id}")
    private String cashfreeClientId;

    @Value("${cashfree.client.secret}")
    private String cashfreeClientSecret;

    @Value("${cashfree.base.url}")
    private String cashfreeBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Create order in Cashfree and get payment link
    public Map<String, String> initiatePayment(int orderId) {
        Map<String, Object> order = orderRepository.getOrderById(orderId);
        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException("Order not found for order ID: " + orderId);
        }

        BigDecimal totalAmount = (BigDecimal) order.get("total_amount");

        // Construct headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-version", "2022-09-01"); // required by Cashfree
        headers.set("x-client-id", cashfreeClientId);
        headers.set("x-client-secret", cashfreeClientSecret);

        System.out.println("Using Client ID: " + cashfreeClientId);
        System.out.println("Using Client Secret: " + cashfreeClientSecret);


        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("order_id", String.valueOf(orderId));
        body.put("order_amount", totalAmount);
        body.put("order_currency", "INR");

        Map<String, String> customerDetails = new HashMap<>();
        customerDetails.put("customer_id", "cust_" + order.get("user_id"));
        customerDetails.put("customer_email", "test@example.com");  // Replace with real email if available
        customerDetails.put("customer_phone", "9999999999");

        body.put("customer_details", customerDetails);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        System.out.println("Request Body: " + body);

        // Hit the Cashfree API
        ResponseEntity<Map> response = restTemplate.postForEntity(
                cashfreeBaseUrl + "/orders",
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to initiate payment via Cashfree: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        System.out.println("🔥 Full Cashfree response: " + responseBody);


        if (responseBody == null || !responseBody.containsKey("payment_session_id")) {
            throw new RuntimeException("Invalid response from Cashfree: missing payment_session");
        }

// Extract real payment session info
//        String sessionId = (String) responseBody.get("payment_session_id");
//        String paymentLink = "https://sandbox.cashfree.com/pg/checkout?payment_session_id=" + sessionId;
        String paymentSessionId = (String) responseBody.get("payment_session_id");
        String paymentLink = "https://sandbox.cashfree.com/pg/orders/" + orderId + "/payments";

        if (paymentLink == null || paymentLink.isEmpty()) {
            throw new RuntimeException("Payment link missing in Cashfree response");
        }

// ✅ Return proper payment link to frontend
        Map<String, String> result = new HashMap<>();
        result.put("order_id", String.valueOf(orderId));
        result.put("amount", totalAmount.toString());
        result.put("payment_link", paymentLink);

        System.out.println("✔️ Cashfree Payment Link: " + paymentLink); // Log for debug

        return result;

    }

    // Verifies if payment matches order and updates status
    public void verifyAndUpdatePayment(int orderId) {
        Map<String, Object> order = orderRepository.getOrderById(orderId);
        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException("Order not found for order ID: " + orderId);
        }

        Map<String, Object> payment = paymentRepository.getPaymentByOrderId(orderId);
        if (payment == null || payment.isEmpty()) {
            throw new IllegalArgumentException("Payment not found for order ID: " + orderId);
        }

        BigDecimal totalAmount = (BigDecimal) order.get("total_amount");
        BigDecimal paidAmount = (BigDecimal) payment.get("amount");

        if (paidAmount.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("Payment amount does not match order total amount");
        }

        // All good, update status
        paymentRepository.updatePaymentStatus(orderId, "Completed");
        orderRepository.updateOrderStatus(orderId, "Confirmed");
    }
}
