package com.linkuni.backend.controller;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.OnboardingRequest;
import com.linkuni.backend.dto.ShareSpaceProfileRequest;
import com.linkuni.backend.dto.ShareSpaceUsernameRequest;
import com.linkuni.backend.dto.UpdateUserRequest;
import com.linkuni.backend.dto.UserDto;
import com.linkuni.backend.model.User;
import com.linkuni.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            User user = (User) authentication.getPrincipal();
            UserDto userDto = UserDto.fromUser(user);
            return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", userDto));
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
    }
    
    /**
     * Logs out the user by clearing the authentication cookie
     *
     * @param response HTTP response to clear cookie
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {
        logger.info("Logout requested");
        
        // Clear the auth token cookie
        Cookie cookie = new Cookie("token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Delete the cookie
        response.addCookie(cookie);
        
        logger.info("Logout successful");
        return ResponseEntity.ok(ApiResponse.success("Logout successful!", null));
    }
    
    /**
     * Updates user profile information
     *
     * @param request update request with user information
     * @param authentication current authenticated user
     * @return updated user information
     */
    @PutMapping("/update-user")
    public ResponseEntity<ApiResponse> updateUser(@Valid @RequestBody UpdateUserRequest request, 
                                               Authentication authentication) {
        logger.info("Update user profile requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Update user failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = userService.updateUser(user.getUserId(), request);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Completes the user onboarding process
     *
     * @param request onboarding request with user preferences
     * @param authentication current authenticated user
     * @return updated user information
     */
    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse> onboardUser(@Valid @RequestBody OnboardingRequest request,
                                                Authentication authentication) {
        logger.info("User onboarding requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Onboarding failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = userService.onboardUser(user.getUserId(), request);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets a user's connections (followers or followings)
     *
     * @param userId the ID of the user
     * @param connection the type of connection ("followers" or "followings")
     * @param authentication current authenticated user
     * @return list of connected users
     */
    @GetMapping("/{userId}/connections")
    public ResponseEntity<ApiResponse> getUserConnections(@PathVariable UUID userId,
                                                       @RequestParam String connection,
                                                       Authentication authentication) {
        logger.info("Get user connections requested. User ID: {}, Connection type: {}", userId, connection);
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Get connections failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        ApiResponse response = userService.getUserConnections(userId, connection);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Follows a user
     *
     * @param userId the ID of the user to follow
     * @param authentication current authenticated user
     * @return success message
     */
    @PutMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse> followUser(@PathVariable UUID userId,
                                               Authentication authentication) {
        logger.info("Follow user requested. Target User ID: {}", userId);
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Follow user failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User currentUser = (User) authentication.getPrincipal();
        ApiResponse response = userService.followUser(currentUser.getUserId(), userId);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Unfollows a user
     *
     * @param userId the ID of the user to unfollow
     * @param authentication current authenticated user
     * @return success message
     */
    @PutMapping("/{userId}/unfollow")
    public ResponseEntity<ApiResponse> unfollowUser(@PathVariable UUID userId,
                                                 Authentication authentication) {
        logger.info("Unfollow user requested. Target User ID: {}", userId);
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Unfollow user failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User currentUser = (User) authentication.getPrincipal();
        ApiResponse response = userService.unfollowUser(currentUser.getUserId(), userId);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates the user's share space profile type
     *
     * @param request profile type update request
     * @param authentication current authenticated user
     * @return updated user information
     */
    @PutMapping("/update-share-space-profile")
    public ResponseEntity<ApiResponse> updateShareSpaceProfile(@Valid @RequestBody ShareSpaceProfileRequest request,
                                                           Authentication authentication) {
        logger.info("Update share space profile type requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Update share space profile type failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = userService.updateShareSpaceProfileType(user.getUserId(), request);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates the user's share space username
     *
     * @param request username update request
     * @param authentication current authenticated user
     * @return updated user information
     */
    @PutMapping("/update-share-space-username")
    public ResponseEntity<ApiResponse> updateShareSpaceUsername(@Valid @RequestBody ShareSpaceUsernameRequest request,
                                                             Authentication authentication) {
        logger.info("Update share space username requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Update share space username failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = userService.updateShareSpaceUsername(user.getUserId(), request);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deletes the user account and all associated data
     *
     * @param authentication current authenticated user
     * @param response HTTP response to clear cookie
     * @return success message
     */
    @DeleteMapping("/delete-user")
    public ResponseEntity<ApiResponse> deleteUser(Authentication authentication, 
                                               HttpServletResponse response) {
        logger.info("Delete user account requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Delete user failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse apiResponse = userService.deleteUser(user.getUserId());
        
        if ("success".equals(apiResponse.getStatus())) {
            // Clear the auth token cookie
            Cookie cookie = new Cookie("token", null);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0); // Delete the cookie
            response.addCookie(cookie);
        } else {
            return ResponseEntity.badRequest().body(apiResponse);
        }
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Gets a single user by ID
     *
     * @param userId the ID of the user to retrieve
     * @return user information
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable UUID userId) {
        logger.info("Get user by ID requested: {}", userId);
        
        ApiResponse response = userService.getUserById(userId);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(404).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all users
     *
     * @return list of all users
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse> getAllUsers() {
        logger.info("Get all users requested");
        
        ApiResponse response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }
} 