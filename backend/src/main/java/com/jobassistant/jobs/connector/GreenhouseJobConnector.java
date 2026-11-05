package com.jobassistant.jobs.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.dto.ApplicationQuestionDto;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.FileProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conector para a API pública oficial da Greenhouse (Job Board API).
 * Endpoint: GET https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs?content=true
 */
@Component
public class GreenhouseJobConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final String baseApiUrl;
    private final HttpClient httpClient;

    public GreenhouseJobConnector(
            ObjectMapper objectMapper,
            @Value("${app.connectors.greenhouse.base-url:https://boards-api.greenhouse.io/v1/boards}") String baseApiUrl
    ) {
        this.objectMapper = objectMapper;
        this.baseApiUrl = baseApiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean supports(JobPlatform platform) {
        return platform == JobPlatform.GREENHOUSE;
    }

    @Override
    public List<RawDiscoveredJob> fetchJobs(JobSourceEntity source) {
        String token = extractBoardToken(source.getBoardIdentifier());
        String endpointUrl = baseApiUrl.endsWith("/")
                ? baseApiUrl + token + "/jobs?content=true"
                : baseApiUrl + "/" + token + "/jobs?content=true";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new BadRequestException("Job Board da Greenhouse não encontrado para o identificador: " + token);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException("A API da Greenhouse retornou HTTP " + response.statusCode() + ".");
            }

            return parseGreenhouseJobsJson(response.body(), source.getName());
        } catch (BadRequestException | FileProcessingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("Consulta à API da Greenhouse interrompida.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new FileProcessingException("Não foi possível consultar o Job Board da Greenhouse (" + token + ").", e);
        }
    }

    public List<RawDiscoveredJob> parseGreenhouseJobsJson(String jsonBody, String defaultCompanyName) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode jobsNode = root.path("jobs");
        if (!jobsNode.isArray()) {
            return List.of();
        }

        List<RawDiscoveredJob> results = new ArrayList<>();
        for (JsonNode jobNode : jobsNode) {
            String externalId = jobNode.path("id").asText("");
            String title = jobNode.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            String location = jobNode.path("location").path("name").asText("");
            String absoluteUrl = jobNode.path("absolute_url").asText("");
            String rawContent = unescapeHtmlEntities(jobNode.path("content").asText(""));
            if (rawContent.isBlank()) {
                rawContent = title + " - " + location;
            }

            Instant publishedAt = parseInstantOrNull(jobNode.path("updated_at").asText(null));
            List<ApplicationQuestionDto> questions = parseQuestions(jobNode.path("questions"));

            results.add(new RawDiscoveredJob(
                    externalId,
                    JobPlatform.GREENHOUSE,
                    defaultCompanyName,
                    title,
                    rawContent,
                    location,
                    null,
                    null,
                    "",
                    questions,
                    absoluteUrl,
                    publishedAt
            ));
        }
        return results;
    }

    private List<ApplicationQuestionDto> parseQuestions(JsonNode questionsNode) {
        if (!questionsNode.isArray()) {
            return List.of();
        }
        List<ApplicationQuestionDto> list = new ArrayList<>();
        for (JsonNode q : questionsNode) {
            String label = q.path("label").asText("");
            boolean required = q.path("required").asBoolean(false);
            JsonNode fields = q.path("fields");
            String fieldName = label;
            String fieldType = "text";
            List<String> options = new ArrayList<>();

            if (fields.isArray() && !fields.isEmpty()) {
                JsonNode f0 = fields.get(0);
                fieldName = f0.path("name").asText(label);
                fieldType = f0.path("type").asText("text");
                JsonNode values = f0.path("values");
                if (values.isArray()) {
                    for (JsonNode v : values) {
                        options.add(v.path("label").asText(""));
                    }
                }
            }
            if (!label.isBlank()) {
                list.add(new ApplicationQuestionDto(fieldName, label, fieldType, required, options));
            }
        }
        return list;
    }

    private String extractBoardToken(String identifier) {
        String trimmed = identifier.trim();
        if (trimmed.contains("greenhouse.io/")) {
            String[] parts = trimmed.split("greenhouse\\.io/");
            String tail = parts[parts.length - 1];
            if (tail.startsWith("v1/boards/")) {
                tail = tail.substring("v1/boards/".length());
            }
            int slashIdx = tail.indexOf('/');
            return slashIdx > 0 ? tail.substring(0, slashIdx) : tail;
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    private String unescapeHtmlEntities(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
