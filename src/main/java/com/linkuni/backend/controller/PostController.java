package com.linkuni.backend.controller;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.dto.PostDto;
import com.linkuni.backend.dto.PostFilterRequest;
import com.linkuni.backend.dto.PostUploadRequest;
import com.linkuni.backend.model.Summary;
import com.linkuni.backend.model.TextExtract;
import com.linkuni.backend.model.User;
import com.linkuni.backend.repository.SummaryRepository;
import com.linkuni.backend.repository.TextExtractRepository;
import com.linkuni.backend.service.PostService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);
    
    private final PostService postService;
    private final SummaryRepository summaryRepository;
    private final TextExtractRepository textExtractRepository;
    
    public PostController(PostService postService, 
                         SummaryRepository summaryRepository, 
                         TextExtractRepository textExtractRepository) {
        this.postService = postService;
        this.summaryRepository = summaryRepository;
        this.textExtractRepository = textExtractRepository;
    }
    
    /**
     * Uploads a new post with file attachment
     * 
     * @param file the file to upload
     * @param title post title
     * @param desc post description (optional)
     * @param program academic program
     * @param course course code or name
     * @param resourceType type of resource
     * @param authentication current authenticated user
     * @return the created post
     */
    @PostMapping(
        path = "/upload", 
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse> uploadPost(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "desc", required = false) String desc,
            @RequestParam("program") String program,
            @RequestParam("course") String course,
            @RequestParam("resourceType") String resourceType,
            Authentication authentication
    ) {
        logger.info("Post upload requested");
        
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Post upload failed: Not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        
        // Create the request object
        PostUploadRequest request = new PostUploadRequest();
        request.setTitle(title);
        request.setDesc(desc);
        request.setProgram(program);
        request.setCourse(course);
        request.setResourceType(resourceType);
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.uploadPost(user.getUserId(), file, request);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets a post by ID
     * 
     * @param postId the ID of the post
     * @return the post
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse> getPostById(@PathVariable UUID postId) {
        logger.info("Get post by ID requested: {}", postId);
        
        ApiResponse response = postService.getPostById(postId);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(404).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Downloads a file from a post
     * 
     * @param postId the ID of the post containing the file
     * @param authentication current authenticated user
     * @return the file as a stream
     */
    @GetMapping("/download-file/{postId}")
    public ResponseEntity<?> downloadFile(@PathVariable UUID postId, Authentication authentication) {
        logger.info("File download requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("File download failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        try {
            // Get file from post
            PostService.StreamingResponse streamingResponse = postService.downloadFileFromPost(postId);
            
            // Check if post exists
            if (streamingResponse == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Post doesn't exist!"));
            }
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(streamingResponse.getContentType()));
            headers.setContentDispositionFormData("attachment", streamingResponse.getFileName());
            
            // Return file stream
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(streamingResponse.getFileStream()));
            
        } catch (Exception e) {
            logger.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error downloading file: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a post and its associated file
     * 
     * @param postId the ID of the post to delete
     * @param authentication current authenticated user
     * @return response with the result of the operation
     */
    @DeleteMapping("/{postId}/delete")
    public ResponseEntity<ApiResponse> deletePost(@PathVariable UUID postId, Authentication authentication) {
        logger.info("Delete post requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Delete post failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.deletePost(postId, user.getUserId());
        
        if ("error".equals(response.getStatus())) {
            String message = response.getMessage();
            if (message.equals("Post doesn't exist!")) {
                return ResponseEntity.status(404).body(response);
            } else if (message.equals("You're not allowed to delete this post")) {
                return ResponseEntity.status(403).body(response);
            } else if (message.equals("User not found!")) {
                return ResponseEntity.status(404).body(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all posts from the database
     * 
     * @param authentication current authenticated user
     * @return list of all posts
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse> getAllPosts(Authentication authentication) {
        logger.info("Get all posts requested");
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Get all posts failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        ApiResponse response = postService.getAllPosts();
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all posts created by a specific user
     * 
     * @param userId the ID of the user
     * @return list of posts created by the user
     */
    @GetMapping("/all-post/{userId}")
    public ResponseEntity<ApiResponse> getAllPostsByUser(@PathVariable UUID userId) {
        logger.info("Get all posts by user requested for user ID: {}", userId);
        
        ApiResponse response = postService.getPostsByUser(userId);
        
        if ("error".equals(response.getStatus())) {
            if (response.getMessage().contains("User not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all posts saved by a user
     * 
     * @param userId the ID of the user
     * @return list of saved posts
     */
    @GetMapping("/saved/{userId}")
    public ResponseEntity<ApiResponse> getSavedPosts(@PathVariable UUID userId) {
        logger.info("Get saved posts requested for user ID: {}", userId);
        
        ApiResponse response = postService.getSavedPostsByUser(userId);
        
        if ("error".equals(response.getStatus())) {
            if (response.getMessage().contains("User not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates a post
     * 
     * @param postId the ID of the post to update
     * @param title updated title (optional)
     * @param desc updated description (optional)
     * @param program updated program (optional)
     * @param course updated course (optional)
     * @param resourceType updated resource type (optional)
     * @param authentication current authenticated user
     * @return the updated post
     */
    @PutMapping("/{postId}/update")
    public ResponseEntity<ApiResponse> updatePost(
            @PathVariable UUID postId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String desc,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String resourceType,
            Authentication authentication
    ) {
        logger.info("Update post requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Update post failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.updatePost(postId, user.getUserId(), 
                title, desc, program, course, resourceType);
        
        if ("error".equals(response.getStatus())) {
            String message = response.getMessage();
            if (message.equals("Post doesn't exist!")) {
                return ResponseEntity.status(404).body(response);
            } else if (message.equals("You're not authorized to modify this post!")) {
                return ResponseEntity.status(403).body(response);
            } else if (message.equals("No updates provided!")) {
                return ResponseEntity.status(400).body(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Likes or unlikes a post
     * 
     * @param postId the ID of the post
     * @param authentication current authenticated user
     * @return the result of the operation
     */
    @PutMapping("/{postId}/like")
    public ResponseEntity<ApiResponse> likePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        logger.info("Like/unlike post requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Like/unlike post failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.likePost(postId, user.getUserId());
        
        if ("error".equals(response.getStatus())) {
            if (response.getMessage().contains("Post doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Saves or unsaves a post for a user
     * 
     * @param postId the ID of the post
     * @param authentication current authenticated user
     * @return the result of the operation
     */
    @PutMapping("/{postId}/save")
    public ResponseEntity<ApiResponse> savePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        logger.info("Save/unsave post requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Save/unsave post failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.savePost(postId, user.getUserId());
        
        if ("error".equals(response.getStatus())) {
            String message = response.getMessage();
            if (message.contains("Post doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            } else if (message.contains("User doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reports a post and marks it as blacklisted
     * 
     * @param postId the ID of the post
     * @param authentication current authenticated user
     * @return the result of the operation
     */
    @PutMapping("/{postId}/report")
    public ResponseEntity<ApiResponse> reportPost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        logger.info("Report post requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Report post failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        ApiResponse response = postService.reportPost(postId, user.getUserId());
        
        if ("error".equals(response.getStatus())) {
            String message = response.getMessage();
            if (message.contains("Post doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            } else if (message.contains("User doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Filters posts based on multiple criteria
     * 
     * @param filterRequest the filter criteria
     * @return the filtered posts
     */
    @PostMapping(
        path = "/filter",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse> filterPosts(
            @RequestBody PostFilterRequest filterRequest
    ) {
        logger.info("Filter posts requested with criteria: {}", filterRequest);
        
        // Ensure filter request is not null
        if (filterRequest == null) {
            filterRequest = new PostFilterRequest();
        }
        
        ApiResponse response = postService.filterPosts(filterRequest);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generates a presigned URL for file preview
     * 
     * @param postId the ID of the post
     * @param authentication current authenticated user
     * @return the presigned URL
     */
    @GetMapping("/preview/{postId}")
    public ResponseEntity<ApiResponse> getPresignedUrl(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        logger.info("Get presigned URL requested for post ID: {}", postId);
        
        // Check authentication
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Get presigned URL failed: Not authenticated");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Not authenticated"));
        }
        
        ApiResponse response = postService.generatePresignedUrl(postId);
        
        if ("error".equals(response.getStatus())) {
            if (response.getMessage().contains("Post doesn't exist")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get text extraction data for a post
     * 
     * @param postId The ID of the post
     * @return The extracted text and summary
     */
    @GetMapping("/{postId}/extract")
    public ResponseEntity<ApiResponse> getTextExtraction(@PathVariable UUID postId) {
        logger.info("Get text extraction requested for post ID: {}", postId);
        
        Map<String, Object> extractionData = new HashMap<>();
        
        // Get summary if available, otherwise set default text
        Optional<Summary> summaryOptional = summaryRepository.findByPost_PostId(postId);
        if (summaryOptional.isPresent()) {
            extractionData.put("summary", summaryOptional.get().getSummaryText());
        } else {
            extractionData.put("summary", "Summary not available");
        }
        
        // Get extracted text if available
        Optional<TextExtract> textExtractOptional = textExtractRepository.findByPost_PostId(postId);
        if (textExtractOptional.isPresent()) {
            extractionData.put("extractedText", textExtractOptional.get().getExtractedText());
        }
        
        if (extractionData.size() <= 1 && !extractionData.containsKey("extractedText")) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success("Text extraction data retrieved", extractionData));
    }
} 