package com.jobassistant.jobs.connector;

import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.dto.ApplicationQuestionDto;

import java.time.Instant;
import java.util.List;

public record RawDiscoveredJob(
        String externalId,
        JobPlatform platform,
        String companyName,
        String title,
        String descriptionRaw,
        String location,
        WorkModality explicitModality,
        String explicitSeniority,
        String salaryRange,
        List<ApplicationQuestionDto> applicationQuestions,
        String originalUrl,
        Instant publishedAt
) {
}
