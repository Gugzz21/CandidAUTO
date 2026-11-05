package com.jobassistant.applications.dto;

import java.util.Map;

public record AnswerMissingQuestionsRequestDto(
        Map<String, String> answers,
        boolean saveToVault
) {
}
