package com.bookstore.api.services;

import com.bookstore.api.dto.AddToCartRequest;
import com.bookstore.api.models.CartItem;
import com.bookstore.api.repositories.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    public void addItemToCart(AddToCartRequest request) {
        Optional<CartItem> existingItem = cartRepository.findByCartIdAndBookId(request.getCartId(), request.getBookId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPrice(request.getPrice()); // Optional: if price might change
            cartRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCartId(request.getCartId());
            newItem.setBookId(request.getBookId());
            newItem.setQuantity(request.getQuantity());
            newItem.setPrice(request.getPrice());
            cartRepository.save(newItem);
        }

        cartRepository.updateCartTotalAmount(request.getCartId());
    }

    public void clearCart(int cartId) {
        cartRepository.deleteAllItemsFromCart(cartId);
        cartRepository.updateCartTotalAmount(cartId);
    }

    public void updateCartTotal(int cartId) {
        cartRepository.updateCartTotalAmount(cartId);
    }

}


