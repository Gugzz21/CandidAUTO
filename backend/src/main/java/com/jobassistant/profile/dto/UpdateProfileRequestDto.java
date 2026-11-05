package com.jobassistant.profile.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateProfileRequestDto(
        @NotBlank(message = "O nome completo é obrigatório.")
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
        List<String> targetRoles
) {
}
