package com.linkuni.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends Auditable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "firstname", nullable = false)
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "share_space_profile_username")
    private String shareSpaceProfileUsername;

    @Column(name = "share_space_profile_type")
    private String shareSpaceProfileType;

    @ElementCollection
    @CollectionTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "follower_id")
    private List<UUID> followers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_followings", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "following_id")
    private List<UUID> followings = new ArrayList<>();

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "is_onboarded")
    private Boolean isOnboarded = false;

    @ElementCollection
    @CollectionTable(name = "user_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "post_id")
    private List<UUID> posts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_blacklisted_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "blacklisted_post_id")
    private List<UUID> blacklistedPosts = new ArrayList<>();

    @Column(name = "program")
    private String program;

    @Column(name = "year_of_graduation")
    private String yearOfGraduation;

    @ElementCollection
    @CollectionTable(name = "user_saved_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "saved_post_id")
    private List<UUID> savedPosts = new ArrayList<>();

    public User() {
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getShareSpaceProfileUsername() {
        return shareSpaceProfileUsername;
    }

    public void setShareSpaceProfileUsername(String shareSpaceProfileUsername) {
        this.shareSpaceProfileUsername = shareSpaceProfileUsername;
    }

    public String getShareSpaceProfileType() {
        return shareSpaceProfileType;
    }

    public void setShareSpaceProfileType(String shareSpaceProfileType) {
        this.shareSpaceProfileType = shareSpaceProfileType;
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

    public List<UUID> getPosts() {
        return posts;
    }

    public void setPosts(List<UUID> posts) {
        this.posts = posts;
    }

    public List<UUID> getBlacklistedPosts() {
        return blacklistedPosts;
    }

    public void setBlacklistedPosts(List<UUID> blacklistedPosts) {
        this.blacklistedPosts = blacklistedPosts;
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

    public List<UUID> getSavedPosts() {
        return savedPosts;
    }

    public void setSavedPosts(List<UUID> savedPosts) {
        this.savedPosts = savedPosts;
    }
} 