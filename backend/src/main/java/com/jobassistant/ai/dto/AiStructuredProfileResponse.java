package com.jobassistant.ai.dto;

import com.jobassistant.profile.dto.CertificationItemDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.ExperienceItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.ProjectItemDto;
import com.jobassistant.profile.dto.SkillItemDto;

import java.util.List;

public record AiStructuredProfileResponse(
        String fullName,
        String fullNameEvidence,
        String headline,
        String headlineEvidence,
        String seniorityLevel,
        String seniorityEvidence,
        String summary,
        String summaryEvidence,
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
