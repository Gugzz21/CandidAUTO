package com.jobassistant.applications.dto;

public record CancelApplicationRequestDto(
        boolean keepAsDraft,
        String reason
) {
}
