package com.linkuni.backend.dto;

import com.linkuni.backend.model.Post;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PostDto {
    private UUID _id;
    private UUID userId;
    private String title;
    private String desc;
    private String fileUrl;
    private String fileKey;
    private String fileType;
    private String fileName;
    private Map<String, String> category;
    private String userFirstName;
    private String userLastName;
    private LocalDateTime createdAt;
    
    public PostDto() {
        this.category = new HashMap<>();
    }
    
    public static PostDto fromPost(Post post) {
        PostDto dto = new PostDto();
        dto.set_id(post.getPostId());
        dto.setUserId(post.getUser().getUserId());
        dto.setTitle(post.getTitle());
        dto.setDesc(post.getDescription());
        dto.setFileUrl(post.getFileUrl());
        dto.setFileKey(post.getFileKey());
        dto.setFileType(post.getFileType());
        dto.setFileName(post.getFileName());
        
        Map<String, String> category = new HashMap<>();
        category.put("program", post.getProgram());
        category.put("course", post.getCourse());
        category.put("resourceType", post.getResourceType());
        dto.setCategory(category);
        
        dto.setUserFirstName(post.getUser().getFirstname());
        dto.setUserLastName(post.getUser().getLastname());
        dto.setCreatedAt(post.getCreatedAt());
        
        return dto;
    }

    public UUID get_id() {
        return _id;
    }

    public void set_id(UUID _id) {
        this._id = _id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, String> getCategory() {
        return category;
    }

    public void setCategory(Map<String, String> category) {
        this.category = category;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 