package com.jobassistant.jobs.dto;

import java.util.List;

public record SyncSourceResponseDto(
        JobSourceDto source,
        int totalFetched,
        int newlyInserted,
        int updatedExisting,
        int markedExpired,
        List<JobPostingDto> jobs,
        ConnectorDiagnosticDto diagnostic
) {
}
