package com.linkuni.backend.dto;

import com.linkuni.backend.model.User;

import java.util.ArrayList;
import java.util.List;
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
    private String professionalProfileType;
    private List<UUID> followers;
    private List<UUID> followings;
    private List<UUID> posts;
    private List<UUID> savedPosts;
    private List<UUID> blacklistedPosts;
    
    public UserDto() {
        this.followers = new ArrayList<>();
        this.followings = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.savedPosts = new ArrayList<>();
        this.blacklistedPosts = new ArrayList<>();
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
        dto.setProfessionalProfileType(user.getShareSpaceProfileType()); // this is the professional profile type
        dto.setFollowers(user.getFollowers());
        dto.setFollowings(user.getFollowings());
        dto.setPosts(user.getPosts());
        dto.setSavedPosts(user.getSavedPosts());
        dto.setBlacklistedPosts(user.getBlacklistedPosts());
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
    
    public String getProfessionalProfileType() {
        return professionalProfileType;
    }
    
    public void setProfessionalProfileType(String professionalProfileType) {
        this.professionalProfileType = professionalProfileType;
    }
    
    public List<UUID> getFollowers() {
        return followers;
    }
    
    public void setFollowers(List<UUID> followers) {
        this.followers = followers;
    }
    
    public List<UUID> getFollowings() {
        return followings;
    }
    
    public void setFollowings(List<UUID> followings) {
        this.followings = followings;
    }
    
    public List<UUID> getPosts() {
        return posts;
    }
    
    public void setPosts(List<UUID> posts) {
        this.posts = posts;
    }
    
    public List<UUID> getSavedPosts() {
        return savedPosts;
    }
    
    public void setSavedPosts(List<UUID> savedPosts) {
        this.savedPosts = savedPosts;
    }

    public List<UUID> getBlacklistedPosts() {
        return blacklistedPosts;
    }

    public void setBlacklistedPosts(List<UUID> blacklistedPosts) {
        this.blacklistedPosts = blacklistedPosts;
    }
} 