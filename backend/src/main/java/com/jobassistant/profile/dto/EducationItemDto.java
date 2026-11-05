package com.jobassistant.profile.dto;

public record EducationItemDto(
        String institution,
        String degree,
        String fieldOfStudy,
        String startDate,
        String endDate,
        String status,
        String evidenceExcerpt
) {
}
