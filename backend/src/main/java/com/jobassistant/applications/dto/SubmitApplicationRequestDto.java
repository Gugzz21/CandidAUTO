package com.jobassistant.applications.dto;

public record SubmitApplicationRequestDto(
        boolean explicitApprovalConfirmed,
        String notes
) {
}
