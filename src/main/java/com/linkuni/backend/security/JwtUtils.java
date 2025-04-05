package com.linkuni.backend.security;

import com.linkuni.backend.dto.SignupRequest;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.activation-expiration}")
    private long activationExpiration;
    
    @Value("${app.jwt.reset-expiration:900000}") // 15 minutes in milliseconds
    private long resetExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email) {
        return createToken(new HashMap<>(), email, jwtExpiration);
    }
    
    public String generateTokenFromUserId(UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        return createToken(claims, userId.toString(), jwtExpiration);
    }

    public String generateActivationToken(SignupRequest signupRequest) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("firstname", signupRequest.getFirstname());
        claims.put("lastname", signupRequest.getLastname());
        claims.put("email", signupRequest.getEmail());
        claims.put("password", signupRequest.getPassword());
        claims.put("username", signupRequest.getEmail().split("@")[0]);
        
        return createToken(claims, signupRequest.getEmail(), activationExpiration);
    }
    
    public String generatePasswordResetToken(UUID userId, String email, String firstname) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("firstname", firstname);
        claims.put("purpose", "password_reset");
        
        return createToken(claims, userId.toString(), resetExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }
    
    public boolean isPasswordResetToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "password_reset".equals(claims.get("purpose"));
        } catch (Exception e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
} 