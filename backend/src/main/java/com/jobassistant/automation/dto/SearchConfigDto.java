package com.jobassistant.automation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchConfigDto(
        UUID id,
        List<String> desiredRoles,
        List<String> desiredModalities,
        List<String> desiredSeniorities,
        int minCompatibilityScore,
        boolean autoMonitoringEnabled,
        int intervalMinutes,
        Instant lastRunAt,
        String lastRunSummary,
        Instant updatedAt
) {
}
