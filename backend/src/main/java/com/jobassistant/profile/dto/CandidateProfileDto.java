package com.jobassistant.profile.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateProfileDto(
        UUID id,
        UUID userId,
        UUID resumeId,
        String fullName,
        String headline,
        String seniorityLevel,
        String summary,
        ContactInfoDto contactInfo,
        List<ExperienceItemDto> experiences,
        List<EducationItemDto> education,
        List<SkillItemDto> skills,
        List<LanguageItemDto> languages,
        List<CertificationItemDto> certifications,
        List<ProjectItemDto> projects,
        List<String> targetRoles,
        List<ProfileEvidenceDto> evidences,
        boolean confirmed,
        Instant confirmedAt,
        Instant updatedAt
) {
}
