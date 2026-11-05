package com.jobassistant.applications.dto;

import com.jobassistant.applications.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequestDto(
        @NotNull(message = "O novo status da candidatura é obrigatório.")
        ApplicationStatus status,
        String notes
) {
}
