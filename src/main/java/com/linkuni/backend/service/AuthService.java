package com.linkuni.backend.service;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.ForgotPasswordRequest;
import com.linkuni.backend.dto.GoogleAuthRequest;
import com.linkuni.backend.dto.LoginRequest;
import com.linkuni.backend.dto.ResetPasswordRequest;
import com.linkuni.backend.dto.SignupRequest;
import com.linkuni.backend.dto.UserDto;
import com.linkuni.backend.model.User;
import com.linkuni.backend.repository.UserRepository;
import com.linkuni.backend.security.JwtUtils;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    
    @Value("${app.valid-domain:example.com}")
    private String validDomain;
    
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                      JwtUtils jwtUtils, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
    }

    public ApiResponse signup(SignupRequest request) {
        // Check if email is already in use
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email is already in use");
        }
        
        // Generate username from email
        String username = request.getEmail().split("@")[0];
        
        // Check if username is already in use
        if (userRepository.existsByUsername(username)) {
            return ApiResponse.error("Username is already in use. Please use a different email.");
        }
        
        // Generate activation token
        String activationToken = jwtUtils.generateActivationToken(request);
        
        // Send activation email
        emailService.sendActivationEmail(request.getEmail(), activationToken);
        
        return ApiResponse.success("Please check your email: " + request.getEmail() + " to activate your account!");
    }
    
    public ApiResponse activateAccount(String activationToken) {
        // Validate token
        if (!jwtUtils.validateToken(activationToken)) {
            return ApiResponse.error("Invalid or expired activation token");
        }
        
        try {
            // Extract claims from token
            Claims claims = jwtUtils.extractClaim(activationToken, claims1 -> claims1);
            
            String email = claims.getSubject();
            String firstname = claims.get("firstname", String.class);
            String lastname = claims.get("lastname", String.class);
            String password = claims.get("password", String.class);
            String username = claims.get("username", String.class);
            
            // Check if user already exists
            if (userRepository.existsByEmail(email)) {
                return ApiResponse.error("User already exists");
            }
            
            // Create new user
            User user = new User();
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setIsAdmin(false);
            user.setIsOnboarded(false);
            user.setFollowers(new ArrayList<>());
            user.setFollowings(new ArrayList<>());
            user.setPosts(new ArrayList<>());
            user.setBlacklistedPosts(new ArrayList<>());
            user.setSavedPosts(new ArrayList<>());
            
            // Save user
            userRepository.save(user);
            
            return ApiResponse.success("Account activated successfully");
        } catch (Exception e) {
            logger.error("Error activating account: {}", e.getMessage());
            return ApiResponse.error("Error activating account: " + e.getMessage());
        }
    }
    
    public ApiResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmail());
        
        // Check if email is in valid domain (if configured)
        if (validDomain != null && !validDomain.isEmpty() && !validDomain.equals("example.com")) {
            String emailDomain = loginRequest.getEmail().split("@")[1];
            if (!emailDomain.equals(validDomain)) {
                logger.warn("Login attempt with invalid domain: {}", emailDomain);
                return ApiResponse.error("Invalid email domain. Only " + validDomain + " is allowed.");
            }
        }
        
        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        
        if (userOptional.isEmpty()) {
            logger.warn("Login failed: User not found for email: {}", loginRequest.getEmail());
            return ApiResponse.error("Invalid email or password");
        }
        
        User user = userOptional.get();
        
        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Login failed: Invalid password for email: {}", loginRequest.getEmail());
            return ApiResponse.error("Invalid email or password");
        }
        
        // Generate JWT token
        String token = jwtUtils.generateTokenFromUserId(user.getUserId());
        
        // Create UserDto to return user data without sensitive information
        UserDto userDto = UserDto.fromUser(user);
        
        // Create response data with both user details and token
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", userDto);
        responseData.put("token", token);
        
        logger.info("Login successful for user: {}", user.getEmail());
        
        // Return success response with token and user data
        return ApiResponse.success("Login successful", responseData);
    }
    
    public ApiResponse googleAuth(GoogleAuthRequest request) {
        logger.info("Google authentication attempt for email: {}", request.getEmail());
        
        // Verify email domain if configured
        if (validDomain != null && !validDomain.isEmpty() && !validDomain.equals("example.com")) {
            String emailDomain = request.getEmail().split("@")[1];
            if (!emailDomain.equals(validDomain)) {
                logger.warn("Google auth attempt with invalid domain: {}", emailDomain);
                return ApiResponse.error("Please use a valid email address");
            }
        }
        
        // Check if user already exists
        Optional<User> existingUserOptional = userRepository.findByEmail(request.getEmail());
        
        User user;
        boolean isNewUser = false;
        
        if (existingUserOptional.isPresent()) {
            // User exists, login flow
            user = existingUserOptional.get();
            logger.info("Existing user found with email: {}", request.getEmail());
        } else {
            // User doesn't exist, signup flow
            isNewUser = true;
            
            // Generate random password
            String randomPassword = generateRandomPassword();
            
            // Extract name components
            String[] nameParts = request.getName().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            
            // Generate username from email
            String username = request.getEmail().split("@")[0];
            
            // Check if username is already in use and modify if needed
            if (userRepository.existsByUsername(username)) {
                username = username + "-" + System.currentTimeMillis() % 10000;
            }
            
            // Create new user
            user = new User();
            user.setFirstname(firstName);
            user.setLastname(lastName);
            user.setEmail(request.getEmail());
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setProfilePicture(request.getGooglePhotoUrl());
            user.setIsAdmin(false);
            user.setIsOnboarded(false);
            user.setFollowers(new ArrayList<>());
            user.setFollowings(new ArrayList<>());
            user.setPosts(new ArrayList<>());
            user.setBlacklistedPosts(new ArrayList<>());
            user.setSavedPosts(new ArrayList<>());
            
            // Save user
            user = userRepository.save(user);
            logger.info("New user created with email: {}", request.getEmail());
        }
        
        // Generate JWT token
        String token = jwtUtils.generateTokenFromUserId(user.getUserId());
        
        // Create UserDto to return user data without sensitive information
        UserDto userDto = UserDto.fromUser(user);
        
        // Create response data with both user details and token
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", userDto);
        responseData.put("token", token);
        responseData.put("isNewUser", isNewUser);
        
        String message = isNewUser ? "User created and logged in successfully" : "User logged in successfully";
        logger.info(message + " for user: {}", user.getEmail());
        
        // Return success response with token and user data
        return ApiResponse.success(message, responseData);
    }
    
    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        logger.info("Password reset requested for email: {}", request.getEmail());
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ApiResponse.error("Please provide your email address");
        }
        
        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            logger.warn("Password reset failed: User not found for email: {}", request.getEmail());
            return ApiResponse.error("User with this email does not exist");
        }
        
        User user = userOptional.get();
        
        // Generate password reset token
        String resetToken = jwtUtils.generatePasswordResetToken(
            user.getUserId(), 
            user.getEmail(), 
            user.getFirstname()
        );
        
        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstname(), resetToken);
            logger.info("Password reset email sent to: {}", user.getEmail());
            
            return ApiResponse.success("A password reset email has been sent to " + user.getEmail() + ". Please check your inbox.");
        } catch (Exception e) {
            logger.error("Error sending password reset email: {}", e.getMessage());
            return ApiResponse.error("Failed to send password reset email: " + e.getMessage());
        }
    }
    
    public ApiResponse resetPassword(String token, ResetPasswordRequest request) {
        logger.info("Password reset attempt with token");
        
        // Validate token
        if (!jwtUtils.validateToken(token) || !jwtUtils.isPasswordResetToken(token)) {
            logger.warn("Password reset failed: Invalid or expired token");
            return ApiResponse.error("Token expired or invalid. Please request a new one.");
        }
        
        try {
            // Extract user ID from token
            UUID userId = jwtUtils.getUserIdFromToken(token);
            
            // Find user
            Optional<User> userOptional = userRepository.findById(userId);
            
            if (userOptional.isEmpty()) {
                logger.warn("Password reset failed: User not found for ID: {}", userId);
                return ApiResponse.error("User not found. Please request a new password reset link.");
            }
            
            User user = userOptional.get();
            
            // Update password
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            
            logger.info("Password reset successful for user: {}", user.getEmail());
            return ApiResponse.success("Password has been reset successfully. You can now log in.");
            
        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage());
            return ApiResponse.error("Error resetting password: " + e.getMessage());
        }
    }
    
    private String generateRandomPassword() {
        StringBuilder firstPart = new StringBuilder(8);
        StringBuilder secondPart = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            firstPart.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            secondPart.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        
        return firstPart.toString() + secondPart.toString();
    }
} 