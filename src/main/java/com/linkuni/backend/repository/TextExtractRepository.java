package com.linkuni.backend.repository;

import com.linkuni.backend.model.Post;
import com.linkuni.backend.model.TextExtract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TextExtractRepository extends JpaRepository<TextExtract, UUID> {
    Optional<TextExtract> findByPost(Post post);
    Optional<TextExtract> findByPost_PostId(UUID postId);
} 