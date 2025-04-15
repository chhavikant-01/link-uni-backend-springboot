package com.linkuni.backend.repository;

import com.linkuni.backend.model.Post;
import com.linkuni.backend.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {
    Optional<Summary> findByPost(Post post);
    Optional<Summary> findByPost_PostId(UUID postId);
} 