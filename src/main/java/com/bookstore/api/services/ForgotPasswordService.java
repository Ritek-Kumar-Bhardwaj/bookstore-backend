package com.bookstore.api.services;

import com.bookstore.api.repositories.UserRepository;
import com.bookstore.api.security.XssSanitizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ForgotPasswordService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private String generateTempPassword() {
        int length = 8;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
        Random random = new Random();
        StringBuilder tempPassword = new StringBuilder();
        for (int i = 0; i < length; i++) {
            tempPassword.append(characters.charAt(random.nextInt(characters.length())));
        }
        return tempPassword.toString();
    }

    public boolean processForgotPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            return false; // Email does not exist
        }

        String tempPassword = generateTempPassword();
        String hashedPassword = XssSanitizer.hashPassword(tempPassword);

        userRepository.updatePassword(email, hashedPassword);

        String subject = "Password Reset Request";
        String message = "Your new temporary password for BookStore Management System is: " + tempPassword + "\nPlease change it after logging in.";
        emailService.sendEmail(email, subject, message);

        return true;
    }
}
