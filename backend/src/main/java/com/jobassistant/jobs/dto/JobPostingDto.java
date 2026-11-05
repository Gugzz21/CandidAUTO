package com.jobassistant.jobs.dto;

import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobPostingDto(
        UUID id,
        UUID sourceId,
        String externalId,
        JobPlatform sourcePlatform,
        String companyName,
        String title,
        String descriptionRaw,
        List<String> mandatoryRequirements,
        List<String> desirableRequirements,
        List<String> technologies,
        String seniority,
        WorkModality workModality,
        String location,
        String salaryRange,
        List<String> benefits,
        List<ApplicationQuestionDto> applicationQuestions,
        String originalUrl,
        Instant publishedAt,
        Instant discoveredAt,
        boolean active
) {
}
