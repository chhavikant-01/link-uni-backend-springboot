package com.linkuni.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

@Service
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.endpoint}")
    private String endpoint;
    
    public S3Service(
            @Value("${aws.region}") String region,
            @Value("${aws.s3.access-key-id}") String accessKeyId,
            @Value("${aws.s3.secret-access-key}") String secretAccessKey) {
        
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        
        logger.info("S3 client initialized with region: {}", region);
    }
    
    /**
     * Uploads a file to S3 bucket
     * 
     * @param file the file to upload
     * @return S3FileDetails containing file key and URL
     * @throws IOException if file cannot be read
     */
    public S3FileDetails uploadFile(MultipartFile file) throws IOException {
        // Generate a unique key for the file
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String fileKey = timestamp + "__" + sanitizeFileName(originalFileName);
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(file.getContentType())
                .build();
        
        PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
        );
        
        logger.info("File uploaded to S3: {}, ETag: {}", fileKey, response.eTag());
        
        String fileUrl = endpoint + "/" + bucketName + "/" + fileKey;
        
        return new S3FileDetails(fileKey, fileUrl);
    }
    
    /**
     * Downloads a file from S3 bucket
     * 
     * @param fileKey the key of the file to download
     * @return InputStream of the file content
     * @throws IOException if the file cannot be downloaded
     */
    public InputStream downloadFile(String fileKey) throws IOException {
        logger.info("Downloading file from S3: {}", fileKey);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            
            return s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
        } catch (Exception e) {
            logger.error("Error downloading file from S3: {}", e.getMessage(), e);
            throw new IOException("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes a file from S3 bucket
     * 
     * @param fileKey the key of the file to delete
     * @throws IOException if the file cannot be deleted
     */
    public void deleteFile(String fileKey) throws IOException {
        logger.info("Deleting file from S3: {}", fileKey);
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("File deleted from S3: {}", fileKey);
        } catch (Exception e) {
            logger.error("Error deleting file from S3: {}", e.getMessage(), e);
            throw new IOException("Failed to delete file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a presigned URL for temporary file access
     * 
     * @param fileKey the key of the file
     * @param expirationMinutes how long the URL should be valid (in minutes)
     * @return presigned URL for direct file access
     */
    public String generatePresignedUrl(String fileKey, int expirationMinutes) {
        logger.info("Generating presigned URL for file: {}", fileKey);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            logger.info("Presigned URL generated: {}", presignedUrl);
            return presignedUrl;
        } catch (Exception e) {
            logger.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage());
        }
    }
    
    /**
     * Sanitizes a file name to be safe for S3 storage
     * 
     * @param fileName the file name to sanitize
     * @return sanitized file name
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unnamed";
        }
        // Replace spaces with underscores and remove special characters
        return fileName.replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_.-]", "");
    }
    
    /**
     * Class to hold S3 file details
     */
    public static class S3FileDetails {
        private final String fileKey;
        private final String fileUrl;
        
        public S3FileDetails(String fileKey, String fileUrl) {
            this.fileKey = fileKey;
            this.fileUrl = fileUrl;
        }
        
        public String getFileKey() {
            return fileKey;
        }
        
        public String getFileUrl() {
            return fileUrl;
        }
    }
} 