package com.bookstore.api.services;

import com.bookstore.api.dto.CartBookDetails;
import com.bookstore.api.dto.LoginResponse;
import com.bookstore.api.dto.UpdateUserRequest;
import com.bookstore.api.models.Admin;

import com.bookstore.api.models.Book;
import com.bookstore.api.models.CartItem;
import com.bookstore.api.models.User;
import com.bookstore.api.repositories.AdminRepository;
import com.bookstore.api.repositories.CartRepository;
import com.bookstore.api.repositories.UserRepository;
import com.bookstore.api.security.XssSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookService bookService;


    public void registerUser(User user) {
        // Sanitize inputs
        user.setName(XssSanitizer.sanitizeName(user.getName()));
        user.setEmail(XssSanitizer.sanitizeEmail(user.getEmail()));
        user.setContactNumber(XssSanitizer.sanitizeContactNumber(user.getContactNumber()));
        user.setAddress(XssSanitizer.sanitizeAddress(user.getAddress()));

        // Hash the password before saving
        user.setPassword(XssSanitizer.hashPassword(user.getPassword()));

        // Save user
        userRepository.save(user);
    }

//    public String loginUser(String email, String password) {
//        // Sanitize inputs
//        email = XssSanitizer.sanitizeEmail(email);
//
//        // Check Admin table
//        Optional<Admin> adminOptional = adminRepository.findByEmail(email);
//        if (adminOptional.isPresent()) {
//            Admin admin = adminOptional.get();
//            if (BCrypt.checkpw(password, admin.getPassword())) {
//                return "ADMIN";  // Return role
//            }
//        }
//
//        // Check User table
//        Optional<User> userOptional = userRepository.findByEmail(email);
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//            if (BCrypt.checkpw(password, user.getPassword())) {
//                return "CUSTOMER";  // Return role
//            }
//        }
//
//        return null; // Login failed
//    }
//
//    public String loginUser(String email, String password) {
//        email = XssSanitizer.sanitizeEmail(email);
//
//        Optional<Admin> adminOptional = adminRepository.findByEmail(email);
//        if (adminOptional.isPresent()) {
//            Admin admin = adminOptional.get();
//            if (BCrypt.checkpw(password, admin.getPassword())) {
//                return "ADMIN";
//            }
//        }
//
//        Optional<User> userOptional = userRepository.findByEmail(email);
//        if (userOptional.isPresent()) {
//            User user = userOptional.get();
//            if (BCrypt.checkpw(password, user.getPassword())) {
//                int userId = user.getUserId();
////                System.out.println("userid - "+user.getName());
////                System.out.println("userid - "+userId);
//
//                Integer cartId = cartRepository.findLatestCartIdByUserId(userId);
////                System.out.println("cart id - "+cartId);
//
//
//                if (cartId == null) {
//                    cartRepository.createCart(userId);
//                } else {
//                    String status = cartRepository.getCartStatus(cartId);
//                    if ("Completed".equalsIgnoreCase(status)) {
//                        cartRepository.createCart(userId);
//                    } else {
//                        List<CartItem> items = cartRepository.getCartItemsByCartId(cartId);
//                        for (CartItem item : items) {
//                            System.out.println("Book ID: " + item.getBookId());
//                            System.out.println("Quantity: " + item.getQuantity());
//                            System.out.println("Price: " + item.getPrice());
//                            System.out.println("Added At: " + item.getAddedAt());
//                            System.out.println("--------------------------");
//                        }
//                    }
//                }
////                System.out.println("\n last Success ");
//                return "CUSTOMER";
//            }
//        }
//
//        return null;
//    }

public LoginResponse loginUser(String email, String password) {
    email = XssSanitizer.sanitizeEmail(email);

    Optional<Admin> adminOptional = adminRepository.findByEmail(email);
    if (adminOptional.isPresent()) {
        Admin admin = adminOptional.get();
        if (BCrypt.checkpw(password, admin.getPassword())) {
            LoginResponse response = new LoginResponse();
            response.setRole("ADMIN");
//            response.setUserId(admin.getAdminId());
            response.setCartId(0); // Admin doesn't have cart, so set to 0 or -1
            response.setCartItems(null); // Admin doesn't need cart items
            return response;
        }
    }

    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isPresent()) {
        User user = userOptional.get();
        if (BCrypt.checkpw(password, user.getPassword())) {
            int userId = user.getUserId();

            Integer cartId = cartRepository.findLatestCartIdByUserId(userId);
            List<CartItem> items = new ArrayList<>();

            if (cartId == null) {
                System.out.println("coo");
                cartId = cartRepository.createCart(userId);
                System.out.println(cartId);
            } else {
                String status = cartRepository.getCartStatus(cartId);
                System.out.println(status);
                if ("Completed".equalsIgnoreCase(status)) {
                    cartId = cartRepository.createCart(userId);
                } else {
                    items = cartRepository.getCartItemsByCartId(cartId);
                }
            }

            List<CartBookDetails> enrichedCartItems = new ArrayList<>();
            for (CartItem item : items) {
                Book book = bookService.getBookCache().get(item.getBookId());
                if (book != null) {
                    CartBookDetails details = new CartBookDetails();
                    details.setBook(book);
                    details.setQuantity(item.getQuantity());
                    details.setPrice(item.getPrice());
                    details.setImageUrl(bookService.getCoverImageByBookId(item.getBookId()));
                    enrichedCartItems.add(details);
                }
            }

            LoginResponse response = new LoginResponse();
            response.setRole("CUSTOMER");
            response.setUserId(userId);
            response.setCartId(cartId);
            response.setCartItems(enrichedCartItems);
            return response;
        }
    }

    return null;
}




    public User getUserByEmail(String email) {
        return userRepository.findByEmail(XssSanitizer.sanitizeEmail(email)).orElse(null);
    }

    public int updateUser(String email, UpdateUserRequest updatedUser) {
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // ✅ Only update fields that are provided
//            if (updatedUser.getName() != null) {
            existingUser.setName(XssSanitizer.sanitizeName(updatedUser.getName()));
//            }
//            if (updatedUser.getAddress() != null) {
            existingUser.setAddress(XssSanitizer.sanitizeAddress(updatedUser.getAddress())); // Encrypt password
//            }
//            if (updatedUser.getContactNumber() != null) {
            existingUser.setContactNumber(XssSanitizer.sanitizeContactNumber(updatedUser.getContactNumber())); // Encrypt password
//            }

            return userRepository.update(existingUser, email); // Save updated user
        }

        return 0; // User not found
    }

}
