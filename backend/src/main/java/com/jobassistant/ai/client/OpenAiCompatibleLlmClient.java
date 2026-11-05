package com.jobassistant.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.ai.dto.AiStructuredProfileResponse;
import com.jobassistant.ai.prompt.ResumeExtractionPromptBuilder;
import com.jobassistant.shared.FileProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
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
            String apiUrl,
            String apiKey,
            String model,
            int timeoutSeconds,
            ObjectMapper objectMapper,
            ResumeExtractionPromptBuilder promptBuilder,
            DeterministicResumeAnalyzer deterministicAnalyzer
    ) {
        this(apiUrl, apiKey, "", "", "", "", model, timeoutSeconds, objectMapper, promptBuilder, deterministicAnalyzer);
    }

    @Autowired
    public OpenAiCompatibleLlmClient(
            @Value("${app.llm.api-url:https://integrate.api.nvidia.com/v1/chat/completions}") String apiUrl,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${CandidAUTO_API_KEY:${CANDIDAUTO_API_KEY:}}") String candidAutoApiKey,
            @Value("${NVIDIA_API_KEY:}") String nvidiaApiKey,
            @Value("${NVIDIA_API_URL:}") String nvidiaApiUrl,
            @Value("${NVIDIA_MODEL:}") String nvidiaModel,
            @Value("${app.llm.model:meta/llama-3.1-70b-instruct}") String model,
            @Value("${app.llm.timeout-seconds:45}") int timeoutSeconds,
            ObjectMapper objectMapper,
            ResumeExtractionPromptBuilder promptBuilder,
            DeterministicResumeAnalyzer deterministicAnalyzer
    ) {
        String resolvedKey = (candidAutoApiKey != null && !candidAutoApiKey.isBlank()) ? candidAutoApiKey.trim()
                : (nvidiaApiKey != null && !nvidiaApiKey.isBlank()) ? nvidiaApiKey.trim()
                : (apiKey != null ? apiKey.trim() : "");
        String resolvedUrl = (nvidiaApiUrl != null && !nvidiaApiUrl.isBlank()) ? nvidiaApiUrl.trim()
                : ((apiUrl != null && !apiUrl.isBlank()) ? apiUrl.trim() : "https://integrate.api.nvidia.com/v1/chat/completions");
        String resolvedModel = (nvidiaModel != null && !nvidiaModel.isBlank()) ? nvidiaModel.trim()
                : ((model != null && !model.isBlank()) ? model.trim() : "meta/llama-3.1-70b-instruct");

        this.apiUrl = resolvedUrl;
        this.apiKey = resolvedKey;
        this.model = resolvedModel;
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
            log.info("Nenhuma chave de API externa (LLM_API_KEY / NVIDIA_API_KEY) configurada; executando extrator estruturado determinístico baseado em evidências literais.");
            return deterministicAnalyzer.analyze(resumeText);
        }

        try {
            HttpResponse<String> response = sendChatCompletionRequest(resumeText, true);
            // Alguns modelos na API NVIDIA NIM não aceitam response_format=json_object e retornam HTTP 400/422
            if (response.statusCode() == 400 || response.statusCode() == 422) {
                log.info("Modelo {} retornou HTTP {} com response_format=json_object; repetindo requisição sem response_format explícito.",
                        model, response.statusCode());
                response = sendChatCompletionRequest(resumeText, false);
            }

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

            String rawJson = extractJsonObjectString(contentNode.asText());
            AiStructuredProfileResponse parsed = objectMapper.readValue(rawJson, AiStructuredProfileResponse.class);
            return mergeWithDeterministicFallbackIfEmpty(parsed, resumeText);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Chamada ao provedor de IA interrompida; acionando extrator determinístico de contingência.");
            return deterministicAnalyzer.analyze(resumeText);
        } catch (IOException | FileProcessingException e) {
            log.warn("Indisponibilidade ou falha no provedor externo de IA ({}); acionando extrator determinístico baseado em evidências literais.", e.getMessage());
            return deterministicAnalyzer.analyze(resumeText);
        }
    }

    private HttpResponse<String> sendChatCompletionRequest(String resumeText, boolean includeJsonResponseFormat)
            throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.0);
        if (includeJsonResponseFormat) {
            payload.put("response_format", Map.of("type", "json_object"));
        }
        payload.put("messages", List.of(
                Map.of("role", "system", "content", promptBuilder.buildSystemPrompt()),
                Map.of("role", "user", "content", promptBuilder.buildUserPrompt(resumeText))
        ));

        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private AiStructuredProfileResponse mergeWithDeterministicFallbackIfEmpty(
            AiStructuredProfileResponse aiResp,
            String resumeText
    ) {
        if (aiResp == null) {
            return deterministicAnalyzer.analyze(resumeText);
        }
        boolean missingCore = (aiResp.fullName() == null || aiResp.fullName().isBlank())
                && (aiResp.skills() == null || aiResp.skills().isEmpty());
        if (missingCore) {
            return deterministicAnalyzer.analyze(resumeText);
        }
        return aiResp;
    }

    private String extractJsonObjectString(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }
}
