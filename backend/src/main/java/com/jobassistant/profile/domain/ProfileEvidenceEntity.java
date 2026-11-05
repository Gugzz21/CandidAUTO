package com.jobassistant.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_evidences")
public class ProfileEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "field_path", nullable = false, length = 160)
    private String fieldPath;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Column(name = "extracted_value", columnDefinition = "TEXT")
    private String extractedValue;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProfileEvidenceEntity() {
    }

    public ProfileEvidenceEntity(
            UUID id,
            UUID profileId,
            String fieldPath,
            String fieldLabel,
            String extractedValue,
            String evidenceText,
            boolean verified,
            boolean needsReview,
            Instant createdAt
    ) {
        this.id = id;
        this.profileId = profileId;
        this.fieldPath = fieldPath;
        this.fieldLabel = fieldLabel;
        this.extractedValue = extractedValue;
        this.evidenceText = evidenceText;
        this.verified = verified;
        this.needsReview = needsReview;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public String getExtractedValue() {
        return extractedValue;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
