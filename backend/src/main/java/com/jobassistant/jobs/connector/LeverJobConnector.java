package com.jobassistant.jobs.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.WorkModality;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conector para a API pública oficial da Lever (Postings API).
 * Endpoint: GET https://api.lever.co/v0/postings/{site}?mode=json
 */
@Component
public class LeverJobConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final String baseApiUrl;
    private final HttpClient httpClient;

    public LeverJobConnector(
            ObjectMapper objectMapper,
            @Value("${app.connectors.lever.base-url:https://api.lever.co/v0/postings}") String baseApiUrl
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
        return platform == JobPlatform.LEVER;
    }

    @Override
    public List<RawDiscoveredJob> fetchJobs(JobSourceEntity source) {
        String site = extractSiteIdentifier(source.getBoardIdentifier());
        String endpointUrl = (baseApiUrl.endsWith("/") ? baseApiUrl : baseApiUrl + "/") + site + "?mode=json";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new FileProcessingException("Limite de requisições da API da Lever atingido (HTTP 429). Aguarde antes de sincronizar novamente.");
            }
            if (response.statusCode() == 404) {
                throw new BadRequestException("Quadro de vagas da Lever não encontrado para o site: " + site);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException("A API da Lever retornou HTTP " + response.statusCode() + ".");
            }

            return parseLeverPostingsJson(response.body(), source.getName());
        } catch (BadRequestException | FileProcessingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("Consulta à API da Lever interrompida.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new FileProcessingException("Não foi possível consultar a API da Lever (" + site + ").", e);
        }
    }

    public List<RawDiscoveredJob> parseLeverPostingsJson(String jsonBody, String defaultCompanyName) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        if (!root.isArray()) {
            return List.of();
        }

        List<RawDiscoveredJob> results = new ArrayList<>();
        for (JsonNode node : root) {
            String externalId = node.path("id").asText("");
            String title = node.path("text").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            String location = node.path("categories").path("location").asText("");
            String workplaceType = node.path("workplaceType").asText("").toLowerCase(Locale.ROOT);
            WorkModality modality = switch (workplaceType) {
                case "remote" -> WorkModality.REMOTO;
                case "hybrid" -> WorkModality.HIBRIDO;
                case "onsite" -> WorkModality.PRESENCIAL;
                default -> null;
            };

            StringBuilder fullDesc = new StringBuilder();
            String descPlain = node.path("descriptionPlain").asText("");
            if (!descPlain.isBlank()) {
                fullDesc.append(descPlain).append("\n\n");
            } else {
                fullDesc.append(node.path("description").asText("")).append("\n\n");
            }

            JsonNode listsNode = node.path("lists");
            if (listsNode.isArray()) {
                for (JsonNode section : listsNode) {
                    String secTitle = section.path("text").asText("");
                    String secContent = section.path("content").asText("");
                    if (!secTitle.isBlank()) {
                        fullDesc.append(secTitle).append(":\n");
                    }
                    if (!secContent.isBlank()) {
                        fullDesc.append(secContent).append("\n\n");
                    }
                }
            }

            String hostedUrl = node.path("hostedUrl").asText("");
            long createdAtMs = node.path("createdAt").asLong(0L);
            Instant publishedAt = createdAtMs > 0 ? Instant.ofEpochMilli(createdAtMs) : null;

            List<ApplicationQuestionDto> standardLeverQuestions = List.of(
                    new ApplicationQuestionDto("name", "Nome Completo", "text", true, List.of()),
                    new ApplicationQuestionDto("email", "E-mail", "email", true, List.of()),
                    new ApplicationQuestionDto("phone", "Telefone", "tel", false, List.of()),
                    new ApplicationQuestionDto("org", "Empresa Atual", "text", false, List.of()),
                    new ApplicationQuestionDto("urls[LinkedIn]", "LinkedIn URL", "url", false, List.of()),
                    new ApplicationQuestionDto("urls[GitHub]", "GitHub URL", "url", false, List.of()),
                    new ApplicationQuestionDto("comments", "Informações Adicionais / Carta de Apresentação", "textarea", false, List.of())
            );

            results.add(new RawDiscoveredJob(
                    externalId,
                    JobPlatform.LEVER,
                    defaultCompanyName,
                    title,
                    fullDesc.toString().trim(),
                    location,
                    modality,
                    null,
                    "",
                    standardLeverQuestions,
                    hostedUrl,
                    publishedAt
            ));
        }
        return results;
    }

    private String extractSiteIdentifier(String identifier) {
        String trimmed = identifier.trim();
        if (trimmed.contains("lever.co/")) {
            String[] parts = trimmed.split("lever\\.co/");
            String tail = parts[parts.length - 1];
            if (tail.startsWith("v0/postings/")) {
                tail = tail.substring("v0/postings/".length());
            }
            int slashOrQuery = tail.indexOf('/');
            if (slashOrQuery > 0) {
                tail = tail.substring(0, slashOrQuery);
            }
            int qIdx = tail.indexOf('?');
            if (qIdx > 0) {
                tail = tail.substring(0, qIdx);
            }
            return tail;
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }
}
