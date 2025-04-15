package com.linkuni.backend.controller;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.ForgotPasswordRequest;
import com.linkuni.backend.dto.GoogleAuthRequest;
import com.linkuni.backend.dto.LoginRequest;
import com.linkuni.backend.dto.ResetPasswordRequest;
import com.linkuni.backend.dto.SignupRequest;
import com.linkuni.backend.model.User;
import com.linkuni.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse> checkAuth(Authentication authentication) {
        logger.info("Authentication check requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Authentication check failed: No authentication found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized! Please log in to continue"));
        }
        
        try {
            User user = (User) authentication.getPrincipal();
            
            if (user == null) {
                logger.warn("Authentication check failed: User not found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("User not found"));
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", user.getUserId());
            
            logger.info("Authentication check successful for user: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Authentication valid", responseData));
        } catch (Exception e) {
            logger.error("Error in authentication check: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Something went wrong: " + e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest request) {
        ApiResponse response = authService.signup(request);
        
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/activation/{token}")
    public ResponseEntity<ApiResponse> activateAccount(@PathVariable String token) {
        ApiResponse response = authService.activateAccount(token);
        
        if ("success".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        ApiResponse apiResponse = authService.login(loginRequest);
        
        if ("success".equals(apiResponse.getStatus())) {
            try {
                // Extract token from response data
                Map<String, Object> responseData = (Map<String, Object>) apiResponse.getData();
                String token = (String) responseData.get("token");
                
                // Create secure HTTP-only cookie
                Cookie cookie = new Cookie("token", token);
                cookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert from milliseconds to seconds
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setSecure(secureCookie); // Set to true in production with HTTPS
                
                // Add cookie to response
                response.addCookie(cookie);
                
                return ResponseEntity.ok(apiResponse);
            } catch (Exception e) {
                logger.error("Error setting token cookie: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Error processing login: " + e.getMessage()));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        }
    }
    
    @PostMapping("/google")
    public ResponseEntity<ApiResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        try {
            logger.info("Google auth request received for email: {}", request.getEmail());
            
            ApiResponse apiResponse = authService.googleAuth(request);
            
            if ("success".equals(apiResponse.getStatus())) {
                // Extract token from response data
                Map<String, Object> responseData = (Map<String, Object>) apiResponse.getData();
                String token = (String) responseData.get("token");
                
                // Create secure HTTP-only cookie
                Cookie cookie = new Cookie("token", token);
                cookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert from milliseconds to seconds
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setSecure(secureCookie); // Set to true in production with HTTPS
                
                // Add cookie to response
                response.addCookie(cookie);
                
                return ResponseEntity.ok(apiResponse);
            } else {
                if (apiResponse.getMessage().contains("valid email")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
            }
        } catch (Exception e) {
            logger.error("Error in Google authentication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password request received for email: {}", request.getEmail());
        
        try {
            ApiResponse apiResponse = authService.forgotPassword(request);
            
            if ("success".equals(apiResponse.getStatus())) {
                return ResponseEntity.ok(apiResponse);
            } else {
                if (apiResponse.getMessage().contains("User with this email does not exist")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
                } else if (apiResponse.getMessage().contains("Please provide your email")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
            }
        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password/{token}")
    public ResponseEntity<ApiResponse> resetPassword(
            @PathVariable String token, 
            @Valid @RequestBody ResetPasswordRequest request) {
        
        logger.info("Password reset request received with token");
        
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Reset token is required"));
        }
        
        try {
            ApiResponse apiResponse = authService.resetPassword(token, request);
            
            if ("success".equals(apiResponse.getStatus())) {
                return ResponseEntity.ok(apiResponse);
            } else {
                if (apiResponse.getMessage().contains("Token expired or invalid")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
                } else if (apiResponse.getMessage().contains("User not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
            }
        } catch (Exception e) {
            logger.error("Error in password reset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {
        // Clear the authentication cookie
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        
        response.addCookie(cookie);
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
} 