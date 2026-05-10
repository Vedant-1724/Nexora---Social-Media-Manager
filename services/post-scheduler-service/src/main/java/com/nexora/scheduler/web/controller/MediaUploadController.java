package com.nexora.scheduler.web.controller;

import com.nexora.scheduler.service.MediaStorageService;
import com.nexora.scheduler.web.dto.MediaUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/posts/media")
public class MediaUploadController {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadController.class);

    private final MediaStorageService mediaStorageService;

    public MediaUploadController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(
            @PathVariable String workspaceId,
            @RequestParam("file") MultipartFile file) {
        try {
            MediaUploadResponse response = mediaStorageService.uploadMedia(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Media upload failed for workspace={}: {}", workspaceId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "MEDIA_UPLOAD_FAILED",
                            "message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during media upload"
                    ));
        }
    }
}
