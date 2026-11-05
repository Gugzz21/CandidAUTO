package com.jobassistant.profile.dto;

public record ContactInfoDto(
        String email,
        String phone,
        String location,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl
) {
    public static ContactInfoDto empty() {
        return new ContactInfoDto("", "", "", "", "", "");
    }
}
