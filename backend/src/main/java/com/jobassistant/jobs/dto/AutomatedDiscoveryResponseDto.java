package com.jobassistant.jobs.dto;

import java.util.List;

public record AutomatedDiscoveryResponseDto(
        int queriesExecuted,
        int syncedUserSources,
        int failedSourcesOrQueries,
        int rawCandidatesEvaluated,
        int relevantJobsMatched,
        int newlyInserted,
        int updatedExisting,
        int filteredOutIrrelevant,
        List<String> activeProviders,
        List<String> warnings,
        List<ConnectorDiagnosticDto> diagnostics,
        List<JobPostingDto> discoveredJobs
) {
}
