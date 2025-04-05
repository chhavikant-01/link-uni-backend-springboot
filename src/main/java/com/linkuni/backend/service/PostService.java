package com.linkuni.backend.service;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.PostDto;
import com.linkuni.backend.dto.PostFilterRequest;
import com.linkuni.backend.dto.PostUploadRequest;
import com.linkuni.backend.model.Post;
import com.linkuni.backend.model.User;
import com.linkuni.backend.repository.PostRepository;
import com.linkuni.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    
    // List of supported file types
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/jpg", "image/webp"
    );
    
    private static final List<String> SUPPORTED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", 
            "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    
    // Maximum file size (10MB)
    @Value("${app.upload.max-file-size:10485760}") // 10MB in bytes
    private long maxFileSize;
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    
    public PostService(PostRepository postRepository, UserRepository userRepository, S3Service s3Service) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }
    
    /**
     * Uploads a new post with an attached file
     * 
     * @param userId the ID of the user uploading the post
     * @param file the file to upload
     * @param request the post metadata
     * @return ApiResponse with the created post
     */
    @Transactional
    public ApiResponse uploadPost(UUID userId, MultipartFile file, PostUploadRequest request) {
        logger.info("Processing post upload for user: {}", userId);
        
        // Validate file
        if (file.isEmpty()) {
            logger.warn("Upload failed: File is empty");
            return ApiResponse.error("Please provide a file");
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            logger.warn("Upload failed: File size exceeds limit. Size: {}", file.getSize());
            return ApiResponse.error("File size exceeds the maximum limit of 10MB");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || (!SUPPORTED_IMAGE_TYPES.contains(contentType) && 
                !SUPPORTED_DOCUMENT_TYPES.contains(contentType))) {
            logger.warn("Upload failed: Unsupported file type: {}", contentType);
            return ApiResponse.error("Unsupported file type. Please upload a PDF, Word document, Excel, PowerPoint, or image file");
        }
        
        // Find user
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Upload failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        
        try {
            // Upload file to S3
            S3Service.S3FileDetails s3FileDetails = s3Service.uploadFile(file);
            
            // Create and save post
            Post post = new Post();
            post.setUser(user);
            post.setTitle(request.getTitle());
            post.setDescription(request.getDesc());
            post.setFileUrl(s3FileDetails.getFileUrl());
            post.setFileKey(s3FileDetails.getFileKey());
            post.setFileType(contentType);
            post.setFileName(file.getOriginalFilename());
            post.setProgram(request.getProgram());
            post.setCourse(request.getCourse());
            post.setResourceType(request.getResourceType());
            
            Post savedPost = postRepository.save(post);
            
            // Add post to user's post list
            user.getPosts().add(savedPost.getPostId());
            userRepository.save(user);
            
            logger.info("Post uploaded successfully. Post ID: {}", savedPost.getPostId());
            
            return ApiResponse.success("Post successfully uploaded!", PostDto.fromPost(savedPost));
            
        } catch (IOException e) {
            logger.error("Error uploading file to S3: {}", e.getMessage(), e);
            return ApiResponse.error("Error uploading file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating post: {}", e.getMessage(), e);
            return ApiResponse.error("Error creating post: " + e.getMessage());
        }
    }
    
    /**
     * Gets a post by ID
     * 
     * @param postId the ID of the post
     * @return ApiResponse with the post
     */
    public ApiResponse getPostById(UUID postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        
        if (postOptional.isEmpty()) {
            logger.warn("Get post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post not found");
        }
        
        Post post = postOptional.get();
        logger.info("Post retrieved: {}", post.getPostId());
        
        return ApiResponse.success("Post retrieved successfully", PostDto.fromPost(post));
    }
    
    /**
     * Downloads a file from a post
     * 
     * @param postId the ID of the post
     * @return a StreamingResponse containing the file data and metadata or null if the file doesn't exist
     * @throws IOException if an error occurs during file download
     */
    public StreamingResponse downloadFileFromPost(UUID postId) throws IOException {
        logger.info("Download file requested for post ID: {}", postId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Download file failed: Post not found with ID: {}", postId);
            return null;
        }
        
        Post post = postOptional.get();
        
        // Validate file key
        String fileKey = post.getFileKey();
        if (fileKey == null || fileKey.isEmpty()) {
            logger.warn("Download file failed: File key is missing in post: {}", post.getPostId());
            return null;
        }
        
        try {
            // Download file from S3
            InputStream fileStream = s3Service.downloadFile(fileKey);
            
            // Create streaming response
            return new StreamingResponse(
                    fileStream,
                    post.getFileType(),
                    post.getFileName()
            );
        } catch (IOException e) {
            logger.error("Error downloading file from S3: {}", e.getMessage(), e);
            throw new IOException("Error downloading file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Class to hold file streaming data
     */
    public static class StreamingResponse {
        private final InputStream fileStream;
        private final String contentType;
        private final String fileName;
        
        public StreamingResponse(InputStream fileStream, String contentType, String fileName) {
            this.fileStream = fileStream;
            this.contentType = contentType != null ? contentType : "application/octet-stream";
            this.fileName = fileName != null ? fileName : "file";
        }
        
        public InputStream getFileStream() {
            return fileStream;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public String getFileName() {
            return fileName;
        }
    }
    
    /**
     * Deletes a post and its associated file
     * 
     * @param postId the ID of the post to delete
     * @param userId the ID of the user requesting the deletion
     * @return ApiResponse with the result of the operation
     */
    @Transactional
    public ApiResponse deletePost(UUID postId, UUID userId) {
        logger.info("Delete post requested for post ID: {} by user ID: {}", postId, userId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Delete post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        Post post = postOptional.get();
        
        // Check if the user is the owner of the post
        if (!post.getUser().getUserId().equals(userId)) {
            logger.warn("Delete post failed: User {} is not the owner of post {}", userId, postId);
            return ApiResponse.error("You're not allowed to delete this post");
        }
        
        // Find user
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Delete post failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found!");
        }
        
        User user = userOptional.get();
        
        try {
            // Delete file from S3 if file key exists
            String fileKey = post.getFileKey();
            if (fileKey != null && !fileKey.isEmpty()) {
                try {
                    s3Service.deleteFile(fileKey);
                } catch (IOException e) {
                    logger.error("Error deleting file from S3: {}", e.getMessage(), e);
                    return ApiResponse.error("Error deleting file: " + e.getMessage());
                }
            }
            
            // Remove post reference from user
            user.getPosts().remove(postId);
            userRepository.save(user);
            
            // Delete post
            postRepository.delete(post);
            
            logger.info("Post deleted successfully: {}", postId);
            
            return ApiResponse.success("The post has been deleted", null);
        } catch (Exception e) {
            logger.error("Error deleting post: {}", e.getMessage(), e);
            return ApiResponse.error("Error deleting post: " + e.getMessage());
        }
    }
    
    /**
     * Gets all posts from the database
     * 
     * @return ApiResponse with the list of all posts
     */
    public ApiResponse getAllPosts() {
        logger.info("Getting all posts");
        
        try {
            List<Post> posts = postRepository.findAll();
            List<PostDto> postDtos = posts.stream()
                    .map(PostDto::fromPost)
                    .toList();
            
            logger.info("Retrieved {} posts", posts.size());
            return ApiResponse.success("Posts retrieved successfully", postDtos);
        } catch (Exception e) {
            logger.error("Error retrieving all posts: {}", e.getMessage(), e);
            return ApiResponse.error("Error retrieving posts: " + e.getMessage());
        }
    }
    
    /**
     * Gets all posts created by a specific user
     * 
     * @param userId the ID of the user
     * @return ApiResponse with the list of posts
     */
    public ApiResponse getPostsByUser(UUID userId) {
        logger.info("Getting posts for user: {}", userId);
        
        // Check if user exists
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Get posts by user failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        try {
            List<Post> posts = postRepository.findByUser_UserId(userId);
            List<PostDto> postDtos = posts.stream()
                    .map(PostDto::fromPost)
                    .toList();
            
            logger.info("Retrieved {} posts for user {}", posts.size(), userId);
            return ApiResponse.success("User posts retrieved successfully", postDtos);
        } catch (Exception e) {
            logger.error("Error retrieving posts for user {}: {}", userId, e.getMessage(), e);
            return ApiResponse.error("Error retrieving user posts: " + e.getMessage());
        }
    }
    
    /**
     * Gets all posts saved by a specific user
     * 
     * @param userId the ID of the user
     * @return ApiResponse with the list of saved posts
     */
    public ApiResponse getSavedPostsByUser(UUID userId) {
        logger.info("Getting saved posts for user: {}", userId);
        
        // Check if user exists
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Get saved posts failed: User not found with ID: {}", userId);
            return ApiResponse.error("User not found");
        }
        
        User user = userOptional.get();
        List<UUID> savedPostIds = user.getSavedPosts();
        
        if (savedPostIds == null || savedPostIds.isEmpty()) {
            logger.info("User {} has no saved posts", userId);
            return ApiResponse.success("User has no saved posts", List.of());
        }
        
        try {
            List<Post> savedPosts = postRepository.findAllById(savedPostIds);
            List<PostDto> postDtos = savedPosts.stream()
                    .map(PostDto::fromPost)
                    .toList();
            
            logger.info("Retrieved {} saved posts for user {}", savedPosts.size(), userId);
            return ApiResponse.success("Saved posts retrieved successfully", postDtos);
        } catch (Exception e) {
            logger.error("Error retrieving saved posts for user {}: {}", userId, e.getMessage(), e);
            return ApiResponse.error("Error retrieving saved posts: " + e.getMessage());
        }
    }
    
    /**
     * Updates a post
     * 
     * @param postId the ID of the post to update
     * @param userId the ID of the user requesting the update
     * @param title updated title (optional)
     * @param description updated description (optional)
     * @param program updated program (optional)
     * @param course updated course (optional)
     * @param resourceType updated resource type (optional)
     * @return ApiResponse with the updated post
     */
    @Transactional
    public ApiResponse updatePost(UUID postId, UUID userId, String title, String description, 
                                  String program, String course, String resourceType) {
        logger.info("Update post requested for post ID: {} by user ID: {}", postId, userId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Update post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        Post post = postOptional.get();
        
        // Check if the user is the owner of the post
        if (!post.getUser().getUserId().equals(userId)) {
            logger.warn("Update post failed: User {} is not the owner of post {}", userId, postId);
            return ApiResponse.error("You're not authorized to modify this post!");
        }
        
        // Check if at least one field is provided for update
        boolean hasUpdates = false;
        
        if (title != null && !title.isBlank()) {
            post.setTitle(title);
            hasUpdates = true;
        }
        
        if (description != null) {
            post.setDescription(description);
            hasUpdates = true;
        }
        
        if (program != null && !program.isBlank()) {
            post.setProgram(program);
            hasUpdates = true;
        }
        
        if (course != null && !course.isBlank()) {
            post.setCourse(course);
            hasUpdates = true;
        }
        
        if (resourceType != null && !resourceType.isBlank()) {
            post.setResourceType(resourceType);
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            logger.warn("Update post failed: No updates provided for post {}", postId);
            return ApiResponse.error("No updates provided!");
        }
        
        try {
            Post updatedPost = postRepository.save(post);
            
            logger.info("Post updated successfully: {}", postId);
            
            return ApiResponse.success("Post updated successfully", PostDto.fromPost(updatedPost));
        } catch (Exception e) {
            logger.error("Error updating post: {}", e.getMessage(), e);
            return ApiResponse.error("Error updating post: " + e.getMessage());
        }
    }
    
    /**
     * Likes or unlikes a post
     * 
     * @param postId the ID of the post
     * @param userId the ID of the user liking/unliking the post
     * @return ApiResponse with the result
     */
    @Transactional
    public ApiResponse likePost(UUID postId, UUID userId) {
        logger.info("Like/unlike post requested for post ID: {} by user ID: {}", postId, userId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Like/unlike post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        Post post = postOptional.get();
        
        try {
            boolean isLiked = post.getLikes().contains(userId);
            
            if (isLiked) {
                // Unlike the post
                post.getLikes().remove(userId);
                postRepository.save(post);
                
                logger.info("Post unliked: {} by user: {}", postId, userId);
                return ApiResponse.success("The post has been disliked", -1);
            } else {
                // Like the post
                post.getLikes().add(userId);
                postRepository.save(post);
                
                logger.info("Post liked: {} by user: {}", postId, userId);
                return ApiResponse.success("The post has been liked", 1);
            }
        } catch (Exception e) {
            logger.error("Error liking/unliking post: {}", e.getMessage(), e);
            return ApiResponse.error("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Saves or unsaves a post for a user
     * 
     * @param postId the ID of the post
     * @param userId the ID of the user
     * @return ApiResponse with the result
     */
    @Transactional
    public ApiResponse savePost(UUID postId, UUID userId) {
        logger.info("Save/unsave post requested for post ID: {} by user ID: {}", postId, userId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Save/unsave post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        // Find user
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Save/unsave post failed: User not found with ID: {}", userId);
            return ApiResponse.error("User doesn't exist!");
        }
        
        User user = userOptional.get();
        
        try {
            boolean isSaved = user.getSavedPosts().contains(postId);
            
            if (isSaved) {
                // Unsave the post
                user.getSavedPosts().remove(postId);
                userRepository.save(user);
                
                logger.info("Post removed from saved: {} by user: {}", postId, userId);
                
                // Create a user DTO without password
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("firstname", user.getFirstname());
                userMap.put("lastname", user.getLastname());
                userMap.put("email", user.getEmail());
                userMap.put("savedPosts", user.getSavedPosts());
                
                return ApiResponse.success("Post has been removed from saved", userMap);
            } else {
                // Save the post
                user.getSavedPosts().add(postId);
                userRepository.save(user);
                
                logger.info("Post saved: {} by user: {}", postId, userId);
                
                // Create a user DTO without password
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("firstname", user.getFirstname());
                userMap.put("lastname", user.getLastname());
                userMap.put("email", user.getEmail());
                userMap.put("savedPosts", user.getSavedPosts());
                
                return ApiResponse.success("Post has been saved", userMap);
            }
        } catch (Exception e) {
            logger.error("Error saving/unsaving post: {}", e.getMessage(), e);
            return ApiResponse.error("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Reports a post and marks it as blacklisted
     * 
     * @param postId the ID of the post
     * @param userId the ID of the user reporting the post
     * @return ApiResponse with the result
     */
    @Transactional
    public ApiResponse reportPost(UUID postId, UUID userId) {
        logger.info("Report post requested for post ID: {} by user ID: {}", postId, userId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Report post failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        // Find user
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Report post failed: User not found with ID: {}", userId);
            return ApiResponse.error("User doesn't exist!");
        }
        
        Post post = postOptional.get();
        User user = userOptional.get();
        
        try {
            // Mark post as blacklisted
            post.setIsBlacklisted(true);
            postRepository.save(post);
            
            // Add post to user's blacklisted posts if not already there
            if (!user.getBlacklistedPosts().contains(postId)) {
                user.getBlacklistedPosts().add(postId);
                userRepository.save(user);
            }
            
            logger.info("Post reported and blacklisted: {} by user: {}", postId, userId);
            return ApiResponse.success("Post has been reported", null);
        } catch (Exception e) {
            logger.error("Error reporting post: {}", e.getMessage(), e);
            return ApiResponse.error("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Filters posts based on multiple criteria
     * 
     * @param filterRequest the filter criteria
     * @return ApiResponse with the filtered posts
     */
    public ApiResponse filterPosts(PostFilterRequest filterRequest) {
        logger.info("Filter posts requested with criteria: {}", filterRequest);
        
        try {
            List<Post> allPosts = postRepository.findAll();
            
            // Apply filters
            List<Post> filteredPosts = allPosts.stream()
                    .filter(post -> {
                        // Program filter
                        if (filterRequest.getProgram() != null && !filterRequest.getProgram().isEmpty() &&
                                !post.getProgram().equalsIgnoreCase(filterRequest.getProgram())) {
                            return false;
                        }
                        
                        // Course filter
                        if (filterRequest.getCourse() != null && !filterRequest.getCourse().isEmpty() &&
                                !post.getCourse().equalsIgnoreCase(filterRequest.getCourse())) {
                            return false;
                        }
                        
                        // Resource type filter
                        if (filterRequest.getResourceType() != null && !filterRequest.getResourceType().isEmpty() &&
                                !post.getResourceType().equalsIgnoreCase(filterRequest.getResourceType())) {
                            return false;
                        }
                        
                        // File type filter
                        if (filterRequest.getFileType() != null && !filterRequest.getFileType().isEmpty() &&
                                !post.getFileType().contains(filterRequest.getFileType())) {
                            return false;
                        }
                        
                        // Keyword search in title or description
                        if (filterRequest.getKeyword() != null && !filterRequest.getKeyword().isEmpty()) {
                            String keyword = filterRequest.getKeyword().toLowerCase();
                            boolean titleMatch = post.getTitle() != null && 
                                    post.getTitle().toLowerCase().contains(keyword);
                            boolean descMatch = post.getDescription() != null && 
                                    post.getDescription().toLowerCase().contains(keyword);
                            
                            if (!titleMatch && !descMatch) {
                                return false;
                            }
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            // Apply sorting if specified
            if (filterRequest.getSort() != null && !filterRequest.getSort().isEmpty()) {
                switch (filterRequest.getSort().toLowerCase()) {
                    case "newest":
                        filteredPosts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
                        break;
                    case "oldest":
                        filteredPosts.sort(Comparator.comparing(Post::getCreatedAt));
                        break;
                    case "title":
                        filteredPosts.sort(Comparator.comparing(Post::getTitle, String.CASE_INSENSITIVE_ORDER));
                        break;
                    // Add more sorting options as needed
                }
            }
            
            // Convert to DTOs
            List<PostDto> postDtos = filteredPosts.stream()
                    .map(PostDto::fromPost)
                    .toList();
            
            logger.info("Filtered posts: returned {} matches", postDtos.size());
            return ApiResponse.success("Posts filtered successfully", postDtos);
        } catch (Exception e) {
            logger.error("Error filtering posts: {}", e.getMessage(), e);
            return ApiResponse.error("Error filtering posts: " + e.getMessage());
        }
    }
    
    /**
     * Generates a presigned URL for a post's file
     * 
     * @param postId the ID of the post
     * @return ApiResponse with the presigned URL
     */
    public ApiResponse generatePresignedUrl(UUID postId) {
        logger.info("Generate presigned URL requested for post ID: {}", postId);
        
        // Find post
        Optional<Post> postOptional = postRepository.findById(postId);
        if (postOptional.isEmpty()) {
            logger.warn("Generate presigned URL failed: Post not found with ID: {}", postId);
            return ApiResponse.error("Post doesn't exist!");
        }
        
        Post post = postOptional.get();
        
        // Check if file key exists
        String fileKey = post.getFileKey();
        if (fileKey == null || fileKey.isEmpty()) {
            logger.warn("Generate presigned URL failed: File key is missing in post: {}", post.getPostId());
            return ApiResponse.error("File not found for this post");
        }
        
        try {
            // Generate presigned URL (valid for 15 minutes)
            String signedUrl = s3Service.generatePresignedUrl(fileKey, 15);
            
            Map<String, String> urlMap = new HashMap<>();
            urlMap.put("signedUrl", signedUrl);
            
            logger.info("Presigned URL generated for post: {}", postId);
            return ApiResponse.success("Presigned URL generated successfully", urlMap);
        } catch (Exception e) {
            logger.error("Error generating presigned URL: {}", e.getMessage(), e);
            return ApiResponse.error("Error generating presigned URL: " + e.getMessage());
        }
    }
} 