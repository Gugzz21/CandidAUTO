package com.jobassistant.automation.dto;

public record DashboardMetricsDto(
        long totalJobsFound,
        long jobsAnalyzedByAi,
        long highCompatibilityJobs,
        long applicationsPrepared,
        long applicationsSubmitted,
        long interviewsScheduled,
        double conversionRatePercent
) {
}
