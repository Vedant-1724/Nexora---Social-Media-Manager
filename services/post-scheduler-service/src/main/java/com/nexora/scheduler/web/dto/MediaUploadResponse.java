package com.nexora.scheduler.web.dto;

import java.util.UUID;

public record MediaUploadResponse(
        String bucketName,
        String objectKey,
        String mimeType,
        String mediaKind,
        long sizeBytes,
        String sha256Checksum,
        String sourceUrl
) {
}
