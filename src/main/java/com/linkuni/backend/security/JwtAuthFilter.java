package com.linkuni.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.model.User;
import com.linkuni.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    public JwtAuthFilter(JwtUtils jwtUtils, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String token = getTokenFromCookies(request);
            
            if (token != null && jwtUtils.validateToken(token)) {
                UUID userId = jwtUtils.getUserIdFromToken(token);
                
                Optional<User> userOptional = userRepository.findById(userId);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            handleAuthenticationException(response, e);
        }
    }
    
    private String getTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
    
    private void handleAuthenticationException(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        
        ApiResponse apiResponse = ApiResponse.error("Unauthorized! Please log in to continue: " + e.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
} 