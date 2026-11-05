package com.jobassistant.resume.dto;

import com.jobassistant.profile.dto.CandidateProfileDto;

public record ResumeAnalyzeResponseDto(
        ResumeResponseDto resume,
        CandidateProfileDto profile
) {
}
