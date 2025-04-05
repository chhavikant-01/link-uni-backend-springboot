package com.linkuni.backend.service;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.OnboardingRequest;
import com.linkuni.backend.dto.ShareSpaceProfileRequest;
import com.linkuni.backend.dto.ShareSpaceUsernameRequest;
import com.linkuni.backend.dto.UpdateUserRequest;
import com.linkuni.backend.dto.UserDto;
import com.linkuni.backend.model.User;
import com.linkuni.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Updates a user's profile information
     * 
     * @param userId the ID of the user to update
     * @param request the update request containing new information
     * @return ApiResponse with the updated user information
     */
    @Transactional
    public ApiResponse updateUser(UUID userId, UpdateUserRequest request) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Update user failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        boolean isPasswordUpdateRequest = request.getPassword() != null && !request.getPassword().isEmpty();
        
        // Only profile picture update (no password verification needed)
        if (request.getProfilePicture() != null && !isPasswordUpdateRequest) {
            user.setProfilePicture(request.getProfilePicture());
            User updatedUser = userRepository.save(user);
            logger.info("Profile picture updated for user: {}", user.getEmail());
            return ApiResponse.success("Profile updated", UserDto.fromUser(updatedUser));
        }
        
        // For other updates, password verification is required
        if (isPasswordUpdateRequest) {
            // Verify current password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.warn("Update user failed: Invalid password for user: {}", user.getEmail());
                return ApiResponse.error("Current password is incorrect");
            }
            
            // Update profile information if present in request
            if (request.getProgram() != null) {
                user.setProgram(request.getProgram());
            }
            
            if (request.getYearOfGraduation() != null) {
                user.setYearOfGraduation(request.getYearOfGraduation());
            }
            
            if (request.getProfessionalProfile() != null) {
                user.setShareSpaceProfileUsername(request.getProfessionalProfile());
            }
            
            // Update password if requested
            if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                logger.info("Password changed for user: {}", user.getEmail());
            }
            
            User updatedUser = userRepository.save(user);
            logger.info("Profile updated for user: {}", user.getEmail());
            return ApiResponse.success("Profile updated", UserDto.fromUser(updatedUser));
        }
        
        return ApiResponse.error("Invalid update request");
    }
    
    /**
     * Completes the user onboarding process
     * 
     * @param userId the ID of the user to onboard
     * @param request the onboarding request
     * @return ApiResponse with the updated user information
     */
    @Transactional
    public ApiResponse onboardUser(UUID userId, OnboardingRequest request) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Onboarding failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        
        // Update user information
        user.setProgram(request.getProgram());
        user.setYearOfGraduation(request.getGraduationYear());
        user.setShareSpaceProfileUsername(request.getUsername());
        user.setIsOnboarded(true);
        
        User updatedUser = userRepository.save(user);
        logger.info("User onboarding completed for: {}", user.getEmail());
        
        return ApiResponse.success("Onboarding successful!", UserDto.fromUser(updatedUser));
    }
    
    /**
     * Gets a user's connections (followers or followings)
     * 
     * @param userId the ID of the user
     * @param connectionType the type of connection ("followers" or "followings")
     * @return ApiResponse with the list of connected users
     */
    public ApiResponse getUserConnections(UUID userId, String connectionType) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Get connections failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        List<UUID> connectionIds;
        
        if ("followers".equalsIgnoreCase(connectionType)) {
            connectionIds = user.getFollowers();
        } else if ("followings".equalsIgnoreCase(connectionType)) {
            connectionIds = user.getFollowings();
        } else {
            logger.warn("Get connections failed: Invalid connection type: {}", connectionType);
            return ApiResponse.error("Invalid connection type. Must be 'followers' or 'followings'");
        }
        
        List<UserDto> connections = connectionIds.stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} {} for user: {}", connections.size(), connectionType, user.getEmail());
        return ApiResponse.success(connectionType + " retrieved successfully", connections);
    }
    
    /**
     * Follows a user
     * 
     * @param followerId the ID of the follower
     * @param targetUserId the ID of the user to follow
     * @return ApiResponse indicating success or failure
     */
    @Transactional
    public ApiResponse followUser(UUID followerId, UUID targetUserId) {
        // Prevent self-follow
        if (followerId.equals(targetUserId)) {
            logger.warn("Follow user failed: User attempted to follow self. User ID: {}", followerId);
            return ApiResponse.error("You can't follow yourself");
        }
        
        Optional<User> followerOptional = userRepository.findById(followerId);
        Optional<User> targetUserOptional = userRepository.findById(targetUserId);
        
        if (followerOptional.isEmpty() || targetUserOptional.isEmpty()) {
            logger.warn("Follow user failed: User not found. Follower ID: {}, Target ID: {}", followerId, targetUserId);
            return ApiResponse.error("User not found");
        }
        
        User follower = followerOptional.get();
        User targetUser = targetUserOptional.get();
        
        // Check if already following
        if (follower.getFollowings().contains(targetUserId)) {
            logger.warn("Follow user failed: Already following. Follower: {}, Target: {}", 
                    follower.getEmail(), targetUser.getEmail());
            return ApiResponse.error("You already follow this user");
        }
        
        // Add to followings and followers
        follower.getFollowings().add(targetUserId);
        targetUser.getFollowers().add(followerId);
        
        userRepository.save(follower);
        userRepository.save(targetUser);
        
        logger.info("User followed successfully. Follower: {}, Target: {}", 
                follower.getEmail(), targetUser.getEmail());
        
        return ApiResponse.success("User has been followed", null);
    }
    
    /**
     * Unfollows a user
     * 
     * @param followerId the ID of the follower
     * @param targetUserId the ID of the user to unfollow
     * @return ApiResponse indicating success or failure
     */
    @Transactional
    public ApiResponse unfollowUser(UUID followerId, UUID targetUserId) {
        // Prevent self-unfollow
        if (followerId.equals(targetUserId)) {
            logger.warn("Unfollow user failed: User attempted to unfollow self. User ID: {}", followerId);
            return ApiResponse.error("You can't unfollow yourself");
        }
        
        Optional<User> followerOptional = userRepository.findById(followerId);
        Optional<User> targetUserOptional = userRepository.findById(targetUserId);
        
        if (followerOptional.isEmpty() || targetUserOptional.isEmpty()) {
            logger.warn("Unfollow user failed: User not found. Follower ID: {}, Target ID: {}", 
                    followerId, targetUserId);
            return ApiResponse.error("User not found");
        }
        
        User follower = followerOptional.get();
        User targetUser = targetUserOptional.get();
        
        // Check if already not following
        if (!follower.getFollowings().contains(targetUserId)) {
            logger.warn("Unfollow user failed: Not following. Follower: {}, Target: {}", 
                    follower.getEmail(), targetUser.getEmail());
            return ApiResponse.error("You do not follow this user");
        }
        
        // Remove from followings and followers
        follower.getFollowings().remove(targetUserId);
        targetUser.getFollowers().remove(followerId);
        
        userRepository.save(follower);
        userRepository.save(targetUser);
        
        logger.info("User unfollowed successfully. Follower: {}, Target: {}", 
                follower.getEmail(), targetUser.getEmail());
        
        return ApiResponse.success("User has been unfollowed", null);
    }
    
    /**
     * Updates a user's share space profile type
     * 
     * @param userId the ID of the user
     * @param request the profile update request
     * @return ApiResponse with the updated user information
     */
    @Transactional
    public ApiResponse updateShareSpaceProfileType(UUID userId, ShareSpaceProfileRequest request) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Update share space profile type failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        
        if (request.getProfile() == null || request.getProfile().isEmpty()) {
            logger.warn("Update share space profile type failed: Profile type not found for user: {}", 
                    user.getEmail());
            return ApiResponse.error("Profile type not found!");
        }
        
        user.setShareSpaceProfileType(request.getProfile());
        User updatedUser = userRepository.save(user);
        
        logger.info("Share space profile type updated for user: {}. New type: {}", 
                user.getEmail(), request.getProfile());
        
        return ApiResponse.success("Profile updated!", UserDto.fromUser(updatedUser));
    }
    
    /**
     * Updates a user's share space username
     * 
     * @param userId the ID of the user
     * @param request the username update request
     * @return ApiResponse with the updated user information
     */
    @Transactional
    public ApiResponse updateShareSpaceUsername(UUID userId, ShareSpaceUsernameRequest request) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Update share space username failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            logger.warn("Update share space username failed: Username not found for user: {}", 
                    user.getEmail());
            return ApiResponse.error("Profile username not found!");
        }
        
        user.setShareSpaceProfileUsername(request.getUsername());
        User updatedUser = userRepository.save(user);
        
        logger.info("Share space username updated for user: {}. New username: {}", 
                user.getEmail(), request.getUsername());
        
        return ApiResponse.success("Profile updated!", UserDto.fromUser(updatedUser));
    }
    
    /**
     * Deletes a user and their associated data
     * 
     * @param userId the ID of the user to delete
     * @return ApiResponse indicating success or failure
     */
    @Transactional
    public ApiResponse deleteUser(UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Delete user failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        
        // Delete the user
        userRepository.delete(user);
        
        // TODO: Delete all posts by the user
        // This will be implemented when Post model and repository are created
        
        logger.info("User deleted: {}", user.getEmail());
        return ApiResponse.success("User deleted!", null);
    }
    
    /**
     * Gets a user by their ID
     * 
     * @param userId the ID of the user to retrieve
     * @return ApiResponse with the user information
     */
    public ApiResponse getUserById(UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.warn("Get user failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        UserDto userDto = UserDto.fromUser(user);
        
        logger.info("User retrieved: {}", user.getEmail());
        return ApiResponse.success("User retrieved successfully", userDto);
    }
    
    /**
     * Gets all users in the system
     * 
     * @return ApiResponse with a list of all users
     */
    public ApiResponse getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
        
        logger.info("Retrieved all users. Count: {}", users.size());
        return ApiResponse.success("Users retrieved successfully", userDtos);
    }
} 