package com.linkuni.backend.repository;

import com.linkuni.backend.model.Post;
import com.linkuni.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByUser(User user);
    List<Post> findByUser_UserId(UUID userId);
    List<Post> findByProgram(String program);
    List<Post> findByCourse(String course);
    List<Post> findByResourceType(String resourceType);
} 