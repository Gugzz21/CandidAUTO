package com.jobassistant.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class ResumeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "file_type", nullable = false, length = 30)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ResumeStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ResumeEntity() {
    }

    public ResumeEntity(
            UUID id,
            UUID userId,
            String filename,
            String storageKey,
            String fileType,
            long fileSizeBytes,
            ResumeStatus status,
            Instant uploadedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.filename = filename;
        this.storageKey = storageKey;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
