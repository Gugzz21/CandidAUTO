package com.jobassistant.applications.dto;

import java.time.Instant;
import java.util.UUID;

public record QaVaultItemDto(
        UUID id,
        String questionKey,
        String questionLabel,
        String confirmedAnswer,
        Instant updatedAt
) {
}
