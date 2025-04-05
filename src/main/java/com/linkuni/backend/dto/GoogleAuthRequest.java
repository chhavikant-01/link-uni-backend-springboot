package com.linkuni.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String googlePhotoUrl;
    
    public GoogleAuthRequest() {
    }
    
    public GoogleAuthRequest(String email, String name, String googlePhotoUrl) {
        this.email = email;
        this.name = name;
        this.googlePhotoUrl = googlePhotoUrl;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getGooglePhotoUrl() {
        return googlePhotoUrl;
    }
    
    public void setGooglePhotoUrl(String googlePhotoUrl) {
        this.googlePhotoUrl = googlePhotoUrl;
    }
} 