package com.jobassistant.applications.dto;

public record MissingQuestionDto(
        String questionKey,
        String questionLabel,
        boolean required,
        String reason
) {
}
