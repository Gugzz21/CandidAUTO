package com.jobassistant.resume.dto;

import com.jobassistant.resume.domain.ResumeStatus;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponseDto(
        UUID id,
        String filename,
        String fileType,
        long fileSizeBytes,
        ResumeStatus status,
        int extractedTextLength,
        String extractedTextPreview,
        String errorMessage,
        Instant uploadedAt
) {
}
