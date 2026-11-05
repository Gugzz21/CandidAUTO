package com.jobassistant.jobs.dto;

import com.jobassistant.jobs.domain.JobPlatform;

import java.time.Instant;
import java.util.UUID;

public record JobSourceDto(
        UUID id,
        String name,
        JobPlatform platform,
        String boardIdentifier,
        boolean enabled,
        Instant lastSyncedAt,
        String lastSyncStatus,
        Instant createdAt
) {
}
