package com.jobassistant.profile.dto;

public record CertificationItemDto(
        String name,
        String issuer,
        String year,
        String evidenceExcerpt
) {
}
