package com.linkuni.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ShareSpaceUsernameRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    public ShareSpaceUsernameRequest() {
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
} 