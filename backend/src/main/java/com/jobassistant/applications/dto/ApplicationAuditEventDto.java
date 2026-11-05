package com.jobassistant.applications.dto;

import java.time.Instant;

public record ApplicationAuditEventDto(
        Instant timestamp,
        String status,
        String action,
        String details
) {
}
