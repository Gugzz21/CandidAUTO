package com.jobassistant.jobs.dto;

import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImportJobRequestDto(
        @NotNull(message = "A plataforma de origem é obrigatória.")
        JobPlatform sourcePlatform,
        @NotBlank(message = "O nome da empresa é obrigatório.")
        String companyName,
        @NotBlank(message = "O título da vaga é obrigatório.")
        String title,
        String location,
        WorkModality workModality,
        String seniority,
        String salaryRange,
        @NotBlank(message = "O link original da oportunidade é obrigatório.")
        String originalUrl,
        @NotBlank(message = "A descrição completa da vaga é obrigatória.")
        String descriptionRaw
) {
}
