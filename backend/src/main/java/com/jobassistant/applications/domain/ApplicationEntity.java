package com.jobassistant.applications.domain;

import com.jobassistant.applications.dto.ApplicationAuditEventDto;
import com.jobassistant.applications.dto.MissingQuestionDto;
import com.jobassistant.applications.dto.PreparedFieldDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class ApplicationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_mode", nullable = false, length = 40)
    private SubmissionMode submissionMode = SubmissionMode.FLUXO_ASSISTIDO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prepared_fields", nullable = false, columnDefinition = "jsonb")
    private List<PreparedFieldDto> preparedFields = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_questions", nullable = false, columnDefinition = "jsonb")
    private List<MissingQuestionDto> missingQuestions = new ArrayList<>();

    @Column(name = "user_approved_explicitly", nullable = false)
    private boolean userApprovedExplicitly = false;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submission_receipt", columnDefinition = "TEXT")
    private String submissionReceipt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_log", nullable = false, columnDefinition = "jsonb")
    private List<ApplicationAuditEventDto> auditLog = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ApplicationEntity() {
    }

    public ApplicationEntity(UUID id, UUID userId, UUID jobId, ApplicationStatus status, SubmissionMode submissionMode) {
        this.id = id;
        this.userId = userId;
        this.jobId = jobId;
        this.status = status;
        this.submissionMode = submissionMode;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void appendAuditEvent(ApplicationStatus status, String action, String details) {
        if (this.auditLog == null) {
            this.auditLog = new ArrayList<>();
        }
        this.auditLog.add(new ApplicationAuditEventDto(Instant.now(), status.name(), action, details));
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public SubmissionMode getSubmissionMode() {
        return submissionMode;
    }

    public void setSubmissionMode(SubmissionMode submissionMode) {
        this.submissionMode = submissionMode;
    }

    public List<PreparedFieldDto> getPreparedFields() {
        return preparedFields;
    }

    public void setPreparedFields(List<PreparedFieldDto> preparedFields) {
        this.preparedFields = preparedFields != null ? preparedFields : new ArrayList<>();
    }

    public List<MissingQuestionDto> getMissingQuestions() {
        return missingQuestions;
    }

    public void setMissingQuestions(List<MissingQuestionDto> missingQuestions) {
        this.missingQuestions = missingQuestions != null ? missingQuestions : new ArrayList<>();
    }

    public boolean isUserApprovedExplicitly() {
        return userApprovedExplicitly;
    }

    public void setUserApprovedExplicitly(boolean userApprovedExplicitly) {
        this.userApprovedExplicitly = userApprovedExplicitly;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmissionReceipt() {
        return submissionReceipt;
    }

    public void setSubmissionReceipt(String submissionReceipt) {
        this.submissionReceipt = submissionReceipt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ApplicationAuditEventDto> getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(List<ApplicationAuditEventDto> auditLog) {
        this.auditLog = auditLog != null ? auditLog : new ArrayList<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
