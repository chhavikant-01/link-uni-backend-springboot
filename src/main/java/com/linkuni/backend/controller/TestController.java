package com.linkuni.backend.controller;

import com.linkuni.backend.dto.ApiResponse;
import com.linkuni.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    private final EmailService emailService;
    private final JavaMailSender mailSender;
    private final Environment environment;
    
    @Value("${spring.mail.username}")
    private String mailUsername;
    
    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private String mailPort;

    public TestController(EmailService emailService, JavaMailSender mailSender, Environment environment) {
        this.emailService = emailService;
        this.mailSender = mailSender;
        this.environment = environment;
    }

    @GetMapping("/send-email")
    public ApiResponse testEmail(@RequestParam String email) {
        logger.info("Email test requested for: {}", email);
        try {
            String testToken = "test-token-" + System.currentTimeMillis();
            emailService.sendActivationEmail(email, testToken);
            return ApiResponse.success("Test email sent to: " + email);
        } catch (Exception e) {
            logger.error("Error in test endpoint: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to send email: " + e.getMessage());
        }
    }
    
    @GetMapping("/email-config")
    public ApiResponse checkEmailConfig() {
        logger.info("Checking email configuration");
        Map<String, Object> config = new HashMap<>();
        
        // Get basic email configuration
        config.put("host", mailHost);
        config.put("port", mailPort);
        config.put("username", mailUsername);
        config.put("auth", environment.getProperty("spring.mail.properties.mail.smtp.auth"));
        config.put("starttls", environment.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        config.put("timeout", environment.getProperty("spring.mail.properties.mail.smtp.timeout"));
        
        // Check if mail sender is initialized
        config.put("mailSenderInitialized", mailSender != null);
        
        return ApiResponse.success("Email configuration retrieved", config);
    }
} 