package com.jobassistant.profile.dto;

import java.util.List;

public record ExperienceItemDto(
        String company,
        String role,
        String startDate,
        String endDate,
        boolean current,
        String description,
        List<String> technologies,
        String evidenceExcerpt
) {
}
