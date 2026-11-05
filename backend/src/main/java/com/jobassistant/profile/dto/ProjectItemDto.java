package com.jobassistant.profile.dto;

import java.util.List;

public record ProjectItemDto(
        String name,
        String description,
        List<String> technologies,
        String url,
        String evidenceExcerpt
) {
}
