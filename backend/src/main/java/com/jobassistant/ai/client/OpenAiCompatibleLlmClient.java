package com.jobassistant.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.ai.dto.AiStructuredProfileResponse;
import com.jobassistant.ai.prompt.ResumeExtractionPromptBuilder;
import com.jobassistant.shared.FileProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final ResumeExtractionPromptBuilder promptBuilder;
    private final DeterministicResumeAnalyzer deterministicAnalyzer;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(
            @Value("${app.llm.api-url:https://generativelanguage.googleapis.com/v1beta/openai/chat/completions}") String apiUrl,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.model:gemini-2.5-flash}") String model,
            @Value("${app.llm.timeout-seconds:45}") int timeoutSeconds,
            ObjectMapper objectMapper,
            ResumeExtractionPromptBuilder promptBuilder,
            DeterministicResumeAnalyzer deterministicAnalyzer
    ) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 5));
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.deterministicAnalyzer = deterministicAnalyzer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiStructuredProfileResponse extractProfileFromResume(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new FileProcessingException("O texto extraído do currículo está vazio.");
        }

        if (apiKey.isEmpty()) {
            log.info("LLM_API_KEY não configurada; executando extrator estruturado determinístico baseado em evidências literais.");
            return deterministicAnalyzer.analyze(resumeText);
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "temperature", 0.0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", promptBuilder.buildSystemPrompt()),
                            Map.of("role", "user", "content", promptBuilder.buildUserPrompt(resumeText))
                    )
            );

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new FileProcessingException("Limite de requisições do provedor de IA atingido (HTTP 429). Tente novamente em instantes.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException("Provedor de IA retornou status HTTP " + response.statusCode() + ".");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new FileProcessingException("Resposta vazia ou malformada recebida do provedor de IA.");
            }

            String rawJson = cleanJsonFence(contentNode.asText());
            return objectMapper.readValue(rawJson, AiStructuredProfileResponse.class);
        } catch (FileProcessingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("A chamada ao provedor de IA foi interrompida.", e);
        } catch (IOException e) {
            throw new FileProcessingException("Falha ao processar resposta estruturada do provedor de IA.", e);
        }
    }

    private String cleanJsonFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
