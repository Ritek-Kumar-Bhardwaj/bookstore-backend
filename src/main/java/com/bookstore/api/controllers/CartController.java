package com.bookstore.api.controllers;

import com.bookstore.api.dto.AddToCartRequest;
import com.bookstore.api.dto.CartItemRequest;
import com.bookstore.api.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

//    @PostMapping("/sync")
//    public ResponseEntity<String> syncCart(
//            @RequestBody List<AddToCartRequest> cartItems,
//            HttpServletRequest request
//    ) {
//        if (cartItems.isEmpty()) {
//            int cartId = Integer.parseInt(request.getHeader("X-Cart-ID"));
//            cartService.clearCart(cartId);
//            return ResponseEntity.ok("Cart cleared");
//        }
//
//        for (AddToCartRequest item : cartItems) {
//            cartService.addItemToCart(item);
//        }
//
////        cartService.updateCartTotal(cartId);
//        return ResponseEntity.ok("Cart synced");
//    }

@PostMapping("/sync")
public ResponseEntity<String> syncCart(
        @RequestBody List<AddToCartRequest> cartItems,
        HttpServletRequest request
) {
    int cartId;

    if (!cartItems.isEmpty()) {
        cartId = cartItems.get(0).getCartId();  // Assuming all items belong to the same cart
    } else {
        cartId = Integer.parseInt(request.getHeader("X-Cart-ID"));  // fallback
    }

    cartService.clearCart(cartId);  // always clear first

    for (AddToCartRequest item : cartItems) {
        cartService.addItemToCart(item);  // will re-add only current items
    }

    cartService.updateCartTotal(cartId);

    return ResponseEntity.ok("Cart synced");
}




}
