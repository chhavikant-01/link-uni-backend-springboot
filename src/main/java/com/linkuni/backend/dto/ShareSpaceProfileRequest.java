package com.linkuni.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ShareSpaceProfileRequest {
    @NotBlank(message = "Profile type is required")
    @Pattern(regexp = "^(personal|professional)$", message = "Profile type must be either 'personal' or 'professional'")
    private String profile;
    
    public ShareSpaceProfileRequest() {
    }
    
    public String getProfile() {
        return profile;
    }
    
    public void setProfile(String profile) {
        this.profile = profile;
    }
} 