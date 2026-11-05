package com.jobassistant.jobs.dto;

import java.util.List;

public record ApplicationQuestionDto(
        String name,
        String label,
        String type,
        boolean required,
        List<String> options
) {
}
