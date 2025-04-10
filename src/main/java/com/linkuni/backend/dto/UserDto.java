package com.linkuni.backend.dto;

import com.linkuni.backend.model.User;

import java.util.UUID;

public class UserDto {
    private UUID userId;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private String profilePicture;
    private Boolean isAdmin;
    private Boolean isOnboarded;
    private String program;
    private String yearOfGraduation;
    private String professionalProfile;
    
    public UserDto() {
    }
    
    public static UserDto fromUser(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setEmail(user.getEmail());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setIsAdmin(user.getIsAdmin());
        dto.setIsOnboarded(user.getIsOnboarded());
        dto.setProgram(user.getProgram());
        dto.setYearOfGraduation(user.getYearOfGraduation());
        dto.setProfessionalProfile(user.getShareSpaceProfileUsername()); // this is the professional profile
        return dto;
    }
    
    // Getters and Setters
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFirstname() {
        return firstname;
    }
    
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }
    
    public String getLastname() {
        return lastname;
    }
    
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getProfilePicture() {
        return profilePicture;
    }
    
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public Boolean getIsOnboarded() {
        return isOnboarded;
    }
    
    public void setIsOnboarded(Boolean isOnboarded) {
        this.isOnboarded = isOnboarded;
    }
    
    public String getProgram() {
        return program;
    }
    
    public void setProgram(String program) {
        this.program = program;
    }
    
    public String getYearOfGraduation() {
        return yearOfGraduation;
    }
    
    public void setYearOfGraduation(String yearOfGraduation) {
        this.yearOfGraduation = yearOfGraduation;
    }
    
    public String getProfessionalProfile() {
        return professionalProfile;
    }
    
    public void setProfessionalProfile(String professionalProfile) {
        this.professionalProfile = professionalProfile;
    }
} 