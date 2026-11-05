package com.jobassistant.jobs.dto;

import com.jobassistant.jobs.domain.JobPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobSourceRequestDto(
        @NotBlank(message = "O nome da fonte é obrigatório.")
        String name,
        @NotNull(message = "A plataforma de origem é obrigatória.")
        JobPlatform platform,
        @NotBlank(message = "O identificador do board ou URL é obrigatório.")
        String boardIdentifier
) {
}
