package com.jobassistant.profile.dto;

import java.util.UUID;

public record ProfileEvidenceDto(
        UUID id,
        String fieldPath,
        String fieldLabel,
        String extractedValue,
        String evidenceText,
        boolean verified,
        boolean needsReview
) {
}
