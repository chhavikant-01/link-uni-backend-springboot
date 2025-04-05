package com.linkuni.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class OnboardingRequest {
    @NotBlank(message = "Program is required")
    private String program;
    
    @NotBlank(message = "Graduation year is required")
    private String graduationYear;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    public OnboardingRequest() {
    }
    
    public String getProgram() {
        return program;
    }
    
    public void setProgram(String program) {
        this.program = program;
    }
    
    public String getGraduationYear() {
        return graduationYear;
    }
    
    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
} 