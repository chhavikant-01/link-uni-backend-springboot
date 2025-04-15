package com.linkuni.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "text_extracts")
public class TextExtract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    private Post post;
    
    // Store page number -> text mapping as JSON
    @Column(columnDefinition = "text")
    private String extractedText;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    public TextExtract() {
    }
    
    public TextExtract(UUID id, Post post, String extractedText, LocalDateTime createdAt) {
        this.id = id;
        this.post = post;
        this.extractedText = extractedText;
        this.createdAt = createdAt;
    }
    
    public static TextExtractBuilder builder() {
        return new TextExtractBuilder();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public String getExtractedText() {
        return extractedText;
    }
    
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Convert page text map to JSON string for storage
     * @param textMap Map of page numbers to page text
     */
    @SuppressWarnings("unchecked")
    public void setTextFromMap(Map<String, Object> textMap) {
        Map<String, String> convertedMap = new HashMap<>();
        
        // Handle different formats of text data in response
        Object textObj = textMap.get("text");
        if (textObj instanceof Map) {
            Map<String, Object> textData = (Map<String, Object>) textObj;
            
            for (Map.Entry<String, Object> entry : textData.entrySet()) {
                // Convert any page numbers to strings
                String pageKey = entry.getKey();
                String pageText = entry.getValue().toString();
                convertedMap.put(pageKey, pageText);
            }
        }
        
        // Convert map to JSON string
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : convertedMap.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append("\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        
        sb.append("}");
        this.extractedText = sb.toString();
    }
    
    /**
     * Escape special characters in JSON string
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    public static class TextExtractBuilder {
        private UUID id;
        private Post post;
        private String extractedText;
        private LocalDateTime createdAt;
        
        TextExtractBuilder() {
        }
        
        public TextExtractBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public TextExtractBuilder post(Post post) {
            this.post = post;
            return this;
        }
        
        public TextExtractBuilder extractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }
        
        public TextExtractBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public TextExtract build() {
            return new TextExtract(id, post, extractedText, createdAt);
        }
    }
} 