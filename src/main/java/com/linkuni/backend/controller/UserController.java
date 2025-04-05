package com.linkuni.backend.controller;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.UserDto;
import com.linkuni.backend.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            User user = (User) authentication.getPrincipal();
            UserDto userDto = UserDto.fromUser(user);
            return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", userDto));
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
    }
} 