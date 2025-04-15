package com.linkuni.backend.dto;

import com.linkuni.backend.model.User;
import java.util.UUID;

public class AuthorDto {
    private UUID userId;
    private String username;
    private String name;
    private String profilePicture;
    private String program;
    private String yearOfGraduation;
    private String professionalProfile;
    private String professionalProfileType;
    private int numberOfPosts;
    private int numberOfFollowers;

    public AuthorDto() {
    }

    public static AuthorDto fromUser(User user) {
        AuthorDto dto = new AuthorDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getFirstname() + " " + user.getLastname());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setProgram(user.getProgram());
        dto.setYearOfGraduation(user.getYearOfGraduation());
        dto.setProfessionalProfile(user.getShareSpaceProfileUsername());
        dto.setProfessionalProfileType(user.getShareSpaceProfileType());
        dto.setNumberOfPosts(user.getPosts().size());
        dto.setNumberOfFollowers(user.getFollowers().size());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
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

    public String getProfessionalProfileType() {
        return professionalProfileType;
    }

    public void setProfessionalProfileType(String professionalProfileType) {
        this.professionalProfileType = professionalProfileType;
    }

    public int getNumberOfPosts() {
        return numberOfPosts;
    }

    public void setNumberOfPosts(int numberOfPosts) {
        this.numberOfPosts = numberOfPosts;
    }

    public int getNumberOfFollowers() {
        return numberOfFollowers;
    }

    public void setNumberOfFollowers(int numberOfFollowers) {
        this.numberOfFollowers = numberOfFollowers;
    }
} 