package com.jobassistant.ai.client;

import com.jobassistant.ai.dto.AiStructuredProfileResponse;

/**
 * Contrato desacoplado de integração com modelo de linguagem para análise e estruturação de currículo.
 */
public interface LlmClient {

    AiStructuredProfileResponse extractProfileFromResume(String resumeText);
}
