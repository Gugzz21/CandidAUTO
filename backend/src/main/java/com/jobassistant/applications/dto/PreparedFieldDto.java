package com.jobassistant.applications.dto;

public record PreparedFieldDto(
        String fieldName,
        String fieldLabel,
        String value,
        String sourceEvidence,
        boolean required,
        boolean requiresUserInput
) {
}
