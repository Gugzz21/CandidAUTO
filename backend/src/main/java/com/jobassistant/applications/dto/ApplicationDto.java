package com.jobassistant.applications.dto;

import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.applications.domain.SubmissionMode;
import com.jobassistant.jobs.dto.JobPostingDto;
import com.jobassistant.matching.dto.JobMatchAnalysisDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationDto(
        UUID id,
        JobPostingDto job,
        JobMatchAnalysisDto matchAnalysis,
        ApplicationStatus status,
        SubmissionMode submissionMode,
        List<PreparedFieldDto> preparedFields,
        List<MissingQuestionDto> missingQuestions,
        boolean userApprovedExplicitly,
        Instant submittedAt,
        String submissionReceipt,
        String notes,
        List<ApplicationAuditEventDto> auditLog,
        Instant createdAt,
        Instant updatedAt
) {
}
