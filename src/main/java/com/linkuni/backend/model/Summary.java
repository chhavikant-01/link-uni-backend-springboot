package com.linkuni.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaries")
public class Summary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "post_id", referencedColumnName = "post_id")
    private Post post;
    
    @Column(columnDefinition = "text")
    private String summaryText;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    public Summary() {
    }
    
    public Summary(UUID id, Post post, String summaryText, LocalDateTime createdAt) {
        this.id = id;
        this.post = post;
        this.summaryText = summaryText;
        this.createdAt = createdAt;
    }
    
    public static SummaryBuilder builder() {
        return new SummaryBuilder();
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
    
    public String getSummaryText() {
        return summaryText;
    }
    
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public static class SummaryBuilder {
        private UUID id;
        private Post post;
        private String summaryText;
        private LocalDateTime createdAt;
        
        SummaryBuilder() {
        }
        
        public SummaryBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public SummaryBuilder post(Post post) {
            this.post = post;
            return this;
        }
        
        public SummaryBuilder summaryText(String summaryText) {
            this.summaryText = summaryText;
            return this;
        }
        
        public SummaryBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Summary build() {
            return new Summary(id, post, summaryText, createdAt);
        }
    }
} 