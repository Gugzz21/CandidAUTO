package com.jobassistant.automation.dto;

import java.util.List;

public record UpdateSearchConfigRequestDto(
        List<String> desiredRoles,
        List<String> desiredModalities,
        List<String> desiredSeniorities,
        int minCompatibilityScore,
        boolean autoMonitoringEnabled,
        int intervalMinutes
) {
}
