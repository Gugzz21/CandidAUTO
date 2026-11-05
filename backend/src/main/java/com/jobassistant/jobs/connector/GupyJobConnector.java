package com.jobassistant.jobs.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.FileProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Conector dedicado para a Gupy combinando:
 * 1) API pública estruturada do Portal Gupy (portal.api.gupy.io/api/v1/jobs)
 * 2) Fallback para dados estruturados da página pública da empresa ({empresa}.gupy.io)
 */
@Component
public class GupyJobConnector implements JobSourceConnector {

    private final ObjectMapper objectMapper;
    private final String portalApiUrl;
    private final CareerPageJsonLdConnector careerPageConnector;
    private final HttpClient httpClient;

    public GupyJobConnector(
            ObjectMapper objectMapper,
            CareerPageJsonLdConnector careerPageConnector,
            @Value("${app.connectors.gupy.portal-api-url:https://portal.api.gupy.io/api/v1/jobs}") String portalApiUrl
    ) {
        this.objectMapper = objectMapper;
        this.careerPageConnector = careerPageConnector;
        this.portalApiUrl = portalApiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean supports(JobPlatform platform) {
        return platform == JobPlatform.GUPY;
    }

    @Override
    public List<RawDiscoveredJob> fetchJobs(JobSourceEntity source) {
        return fetchJobsSafely(source).jobs();
    }

    @Override
    public ConnectorFetchResult fetchJobsSafely(JobSourceEntity source) {
        String subdomain = extractGupySubdomain(source.getBoardIdentifier());
        String pageUrl = resolveGupyPageUrl(source.getBoardIdentifier(), subdomain);

        Map<String, RawDiscoveredJob> deduplicated = new LinkedHashMap<>();
        boolean portalApiSucceeded = false;

        // 1. Tentar consulta JSON estruturada na API pública do Portal Gupy
        try {
            String searchTerm = !source.getName().isBlank() ? source.getName() : subdomain;
            List<RawDiscoveredJob> apiJobs = searchPortalJobs(searchTerm, 30);
            portalApiSucceeded = true;
            String subLower = subdomain.toLowerCase(Locale.ROOT);
            String nameLower = source.getName().toLowerCase(Locale.ROOT);

            for (RawDiscoveredJob job : apiJobs) {
                String urlLower = job.originalUrl().toLowerCase(Locale.ROOT);
                String compLower = job.companyName().toLowerCase(Locale.ROOT);
                if ((!subLower.isBlank() && urlLower.contains(subLower + ".gupy.io"))
                        || (!nameLower.isBlank() && (compLower.contains(nameLower) || nameLower.contains(compLower)))) {
                    deduplicated.putIfAbsent(job.title() + "|" + job.originalUrl(), job);
                }
            }
        } catch (RuntimeException ignored) {
            // Segue para fallback HTML da página de carreiras
        }

        // 2. Fallback / complemento pela página pública da empresa na Gupy
        if (deduplicated.isEmpty() && !pageUrl.isBlank()) {
            try {
                List<RawDiscoveredJob> htmlJobs = careerPageConnector.fetchStructuredJobsFromUrl(
                        pageUrl,
                        JobPlatform.GUPY,
                        source.getName()
                );
                for (RawDiscoveredJob job : htmlJobs) {
                    deduplicated.putIfAbsent(job.title() + "|" + job.originalUrl(), job);
                }
            } catch (RuntimeException ex) {
                if (!portalApiSucceeded) {
                    throw ex;
                }
            }
        }

        List<RawDiscoveredJob> results = new ArrayList<>(deduplicated.values());
        SyncOutcomeStatus outcome = results.isEmpty()
                ? SyncOutcomeStatus.EXTRACTION_INCONCLUSIVE
                : SyncOutcomeStatus.SUCCESS_WITH_RESULTS;
        return new ConnectorFetchResult(
                results,
                false,
                outcome,
                pageUrl.isBlank() ? portalApiUrl : pageUrl,
                200,
                "Conector Gupy híbrido (API JSON + Portal)"
        );
    }

    public ConnectorFetchResult searchPortalJobsWithDiagnostics(String query, int limit) {
        if (query == null || query.isBlank()) {
            return new ConnectorFetchResult(List.of(), true, SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED, portalApiUrl, 200, "Consulta vazia");
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        int safeLimit = Math.max(1, Math.min(50, limit));
        String separator = portalApiUrl.contains("?") ? "&" : "?";
        String url = portalApiUrl + separator + "jobName=" + encoded + "&limit=" + safeLimit + "&offset=0";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("User-Agent", "CandidAUTO/1.0 (+Official-Public-Job-Search)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new ConnectorFetchResult(
                        List.of(),
                        false,
                        SyncOutcomeStatus.HTTP_ERROR,
                        url,
                        response.statusCode(),
                        "Portal API da Gupy retornou HTTP " + response.statusCode() + "."
                );
            }

            List<RawDiscoveredJob> parsed = parsePortalJobsJson(response.body());
            SyncOutcomeStatus outcome = parsed.isEmpty()
                    ? SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED
                    : SyncOutcomeStatus.SUCCESS_WITH_RESULTS;
            return new ConnectorFetchResult(parsed, true, outcome, url, response.statusCode(), "OK");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.NETWORK_ERROR, url, null, "Consulta interrompida");
        } catch (IOException e) {
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.NETWORK_ERROR, url, null, e.getMessage());
        } catch (IllegalArgumentException e) {
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.PARSE_ERROR, url, null, e.getMessage());
        }
    }

    public List<RawDiscoveredJob> searchPortalJobs(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        int safeLimit = Math.max(1, Math.min(50, limit));
        String separator = portalApiUrl.contains("?") ? "&" : "?";
        String url = portalApiUrl + separator + "jobName=" + encoded + "&limit=" + safeLimit + "&offset=0";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("User-Agent", "CandidAUTO/1.0 (+Official-Public-Job-Search)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException("Portal API da Gupy retornou HTTP " + response.statusCode() + ".");
            }

            return parsePortalJobsJson(response.body());
        } catch (FileProcessingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("Consulta ao Portal Gupy interrompida.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new FileProcessingException("Falha ao consultar API pública de vagas da Gupy.", e);
        }
    }

    public List<RawDiscoveredJob> parsePortalJobsJson(String jsonBody) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            return List.of();
        }

        List<RawDiscoveredJob> results = new ArrayList<>();
        for (JsonNode item : dataNode) {
            String id = item.path("id").asText("");
            String title = firstNonBlank(item, "name", "title");
            if (title.isEmpty()) {
                continue;
            }
            String company = firstNonBlank(item, "careerPageName", "companyName");
            if (company.isEmpty()) {
                company = "Empresa na Gupy";
            }
            String description = item.path("description").asText("");
            if (description.isBlank()) {
                description = title + " - " + company;
            }

            String city = item.path("city").asText("").trim();
            String state = item.path("state").asText("").trim();
            String country = item.path("country").asText("").trim();
            String location = buildLocation(city, state, country);

            boolean isRemote = item.path("isRemoteWork").asBoolean(false);
            String workplaceType = item.path("workplaceType").asText("").toLowerCase(Locale.ROOT);
            WorkModality modality = (isRemote || workplaceType.contains("remote"))
                    ? WorkModality.REMOTO
                    : workplaceType.contains("hybrid")
                    ? WorkModality.HIBRIDO
                    : (workplaceType.contains("on-site") || workplaceType.contains("onsite"))
                    ? WorkModality.PRESENCIAL
                    : null;

            String jobUrl = firstNonBlank(item, "jobUrl", "url");
            Instant publishedAt = parseInstantOrNull(item.path("publishedDate").asText(null));

            results.add(new RawDiscoveredJob(
                    id,
                    JobPlatform.GUPY,
                    company,
                    title,
                    description,
                    location,
                    modality,
                    null,
                    "",
                    List.of(),
                    jobUrl,
                    publishedAt
            ));
        }
        return results;
    }

    private String buildLocation(String city, String state, String country) {
        List<String> parts = new ArrayList<>();
        if (!city.isBlank()) parts.add(city);
        if (!state.isBlank()) parts.add(state);
        if (parts.isEmpty() && !country.isBlank()) parts.add(country);
        return String.join(", ", parts);
    }

    private String extractGupySubdomain(String rawIdentifier) {
        if (rawIdentifier == null) {
            return "";
        }
        String trimmed = rawIdentifier.trim();
        if (trimmed.contains(".gupy.io")) {
            String withoutProto = trimmed.replaceFirst("^https?://", "");
            int dotIdx = withoutProto.indexOf(".gupy.io");
            if (dotIdx > 0) {
                return withoutProto.substring(0, dotIdx).replaceAll("[^a-zA-Z0-9\\-]", "");
            }
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return "";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9\\-]", "");
    }

    private String resolveGupyPageUrl(String rawIdentifier, String subdomain) {
        String trimmed = rawIdentifier != null ? rawIdentifier.trim() : "";
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (!subdomain.isBlank()) {
            return "https://" + subdomain + ".gupy.io";
        }
        throw new BadRequestException("Informe o identificador da empresa na Gupy (ex.: itau) ou a URL completa.");
    }

    private String firstNonBlank(JsonNode node, String... fields) {
        for (String field : fields) {
            String val = node.path(field).asText("").trim();
            if (!val.isEmpty()) {
                return val;
            }
        }
        return "";
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (Exception e) {
                return null;
            }
        }
    }
}
