package com.linkuni.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private String mailPort;
    
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.info("EmailService initialized");
    }

    @Async
    public void sendActivationEmail(String to, String activationToken) {
        logger.info("Preparing to send activation email to: {}", to);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Activate Your LinkUni Account");
            logger.debug("Email from: {}, to: {}, subject set", fromEmail, to);
            
            String activationLink = "http://localhost:8080/api/auth/activation/" + activationToken;
            
            String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #4a4a4a;'>Welcome to LinkUni!</h2>"
                    + "<p>Thank you for signing up. Please click the button below to activate your account:</p>"
                    + "<a href='" + activationLink + "' style='display: inline-block; background-color: #4285f4; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin: 15px 0;'>Activate Account</a>"
                    + "<p>If the button doesn't work, you can copy and paste the following link into your browser:</p>"
                    + "<p><a href='" + activationLink + "'>" + activationLink + "</a></p>"
                    + "<p>This link will expire in 5 minutes.</p>"
                    + "<p>If you didn't sign up for a LinkUni account, you can safely ignore this email.</p>"
                    + "<p>Best regards,<br>The LinkUni Team</p>"
                    + "</div>";
            
            helper.setText(htmlMsg, true);
            logger.debug("Email content prepared");
            
            logger.info("Email configuration - Host: {}, Port: {}, Username: {}", mailHost, mailPort, fromEmail);
            logger.info("Attempting to send email now...");
            mailSender.send(mimeMessage);
            logger.info("Activation email successfully sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("MessagingException: Failed to create email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email - messaging error", e);
        } catch (MailException e) {
            logger.error("MailException: Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email - mail server error", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email - unexpected error", e);
        }
    }
    
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetToken) {
        logger.info("Preparing to send password reset email to: {}", to);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Request");
            logger.debug("Email from: {}, to: {}, subject set", fromEmail, to);
            
            // Create reset link using the frontend URL
            String resetLink = frontendUrl + "/reset-password/" + resetToken;
            
            String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #4a4a4a;'>Hello " + firstName + "!</h2>"
                    + "<p>We received a request to reset the password for your LinkUni account.</p>"
                    + "<p>Click the button below to reset your password:</p>"
                    + "<a href='" + resetLink + "' style='display: inline-block; background-color: #4285f4; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin: 15px 0;'>Reset Password</a>"
                    + "<p>If the button doesn't work, you can copy and paste the following link into your browser:</p>"
                    + "<p><a href='" + resetLink + "'>" + resetLink + "</a></p>"
                    + "<p>This link will expire in 15 minutes.</p>"
                    + "<p>If you didn't request a password reset, you can safely ignore this email.</p>"
                    + "<p>Best regards,<br>The LinkUni Team</p>"
                    + "</div>";
            
            helper.setText(htmlMsg, true);
            logger.debug("Password reset email content prepared");
            
            logger.info("Attempting to send password reset email now...");
            mailSender.send(mimeMessage);
            logger.info("Password reset email successfully sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("MessagingException: Failed to create password reset email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email - messaging error", e);
        } catch (MailException e) {
            logger.error("MailException: Failed to send password reset email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email - mail server error", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending password reset email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email - unexpected error", e);
        }
    }
} 