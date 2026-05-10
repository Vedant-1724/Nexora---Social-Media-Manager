package com.nexora.scheduler.service;

import com.nexora.scheduler.web.dto.MediaUploadResponse;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class MediaStorageService {

    private final MinioClient minioClient;
    private final String storageEndpoint;
    private final String defaultBucket;

    public MediaStorageService(
            @Value("${nexora.storage.endpoint}") String endpoint,
            @Value("${nexora.storage.access-key}") String accessKey,
            @Value("${nexora.storage.secret-key}") String secretKey,
            @Value("${nexora.storage.default-bucket}") String defaultBucket) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.storageEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.defaultBucket = defaultBucket;
    }

    public MediaUploadResponse uploadMedia(MultipartFile file) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String objectKey = "media/" + UUID.randomUUID() + extension;
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String mediaKind = mimeType.startsWith("video/") ? "video" : "image";
        
        // Compute SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = file.getBytes();
        byte[] hash = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String checksum = hexString.toString();

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .stream(is, fileBytes.length, -1)
                    .contentType(mimeType)
                    .build());
        }

        String sourceUrl = storageEndpoint + "/" + defaultBucket + "/" + objectKey;

        return new MediaUploadResponse(
                defaultBucket,
                objectKey,
                mimeType,
                mediaKind,
                fileBytes.length,
                checksum,
                sourceUrl
        );
    }
}
