package com.jobassistant.matching.dto;

import com.jobassistant.jobs.dto.JobPostingDto;

import java.util.UUID;

public record OpportunityCardDto(
        JobPostingDto job,
        JobMatchAnalysisDto matchAnalysis,
        UUID applicationId,
        String applicationStatus
) {
}
