package com.linkuni.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Service to extract text from PDFs using the Flask API
 */
@Service
public class ExtractTextService {

    private static final Logger logger = LoggerFactory.getLogger(ExtractTextService.class);
    private final RestTemplate restTemplate;
    
    @Value("${app.flask.base-url:http://127.0.0.1:5000}")
    private String flaskBaseUrl;
    
    private final String extractTextEndpoint = "/api/v1/extract-text";

    public ExtractTextService() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30)) // Longer timeout for text extraction
                .build();
        logger.info("ExtractTextService initialized with Flask API at: {}", flaskBaseUrl + extractTextEndpoint);
    }

    /**
     * Extract text from a PDF file
     *
     * @param file The PDF file to extract text from
     * @return Map containing the extracted text and summary
     */
    public Map<String, Object> extractText(MultipartFile file) {
        logger.info("Extracting text from file: {}", file.getOriginalFilename());
        
        try {
            // Create multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("file", fileResource);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Send request to Flask API
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    flaskBaseUrl + extractTextEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            logger.info("Text extraction completed successfully for file: {}", file.getOriginalFilename());
            return responseEntity.getBody();
            
        } catch (IOException e) {
            logger.error("Error reading file bytes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        } catch (RestClientException e) {
            logger.error("Error calling Flask API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
} 