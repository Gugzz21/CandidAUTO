package com.jobassistant.jobs.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.connector.ConnectorFetchResult;
import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Pesquisa automática de oportunidades remotas globais na API pública oficial da Remotive,
 * registrando URL consultada, código HTTP e resultado estruturado.
 */
@Component
public class RemotivePublicSearchProvider implements QueryJobSearchProvider {

    private final ObjectMapper objectMapper;
    private final JobRelevanceFilter relevanceFilter;
    private final String apiUrl;
    private final HttpClient httpClient;

    public RemotivePublicSearchProvider(
            ObjectMapper objectMapper,
            JobRelevanceFilter relevanceFilter,
            @Value("${app.connectors.remotive.api-url:https://remotive.com/api/remote-jobs}") String apiUrl
    ) {
        this.objectMapper = objectMapper;
        this.relevanceFilter = relevanceFilter;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String providerName() {
        return "Remotive Public API";
    }

    @Override
    public ProviderSearchResult search(DiscoveryCriteria criteria) {
        List<String> queries = relevanceFilter.buildEnglishQueries(criteria);
        List<ProviderQueryExecution> executions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String query : queries) {
            ConnectorFetchResult res = fetchRemotiveWithDiagnostics(query, 25);
            executions.add(new ProviderQueryExecution("Remotive ('" + query + "')", res));
            if (res.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                    || res.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                    || res.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR) {
                warnings.add("Remotive ('" + query + "'): " + res.diagnosticNote());
            }
        }

        return new ProviderSearchResult(providerName(), queries.size(), executions, warnings);
    }

    public ConnectorFetchResult fetchRemotiveWithDiagnostics(String query, int limit) {
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String separator = apiUrl.contains("?") ? "&" : "?";
        String url = apiUrl + separator + "search=" + encoded + "&limit=" + Math.max(1, Math.min(50, limit));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("User-Agent", "CandidAUTO/1.0 (+Official-Remotive-Public-API)")
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
                        "API Remotive retornou HTTP " + response.statusCode()
                );
            }

            List<RawDiscoveredJob> parsed = parseRemotiveJson(response.body());
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

    public List<RawDiscoveredJob> parseRemotiveJson(String jsonBody) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode jobsNode = root.path("jobs");
        if (!jobsNode.isArray()) {
            throw new IOException("JSON sem array 'jobs' esperado na API Remotive");
        }

        List<RawDiscoveredJob> results = new ArrayList<>();
        for (JsonNode item : jobsNode) {
            String id = item.path("id").asText("");
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            String company = item.path("company_name").asText("Empresa Remota").trim();
            String location = item.path("candidate_required_location").asText("Remoto").trim();
            String salary = item.path("salary").asText("").trim();
            String url = item.path("url").asText("").trim();
            String description = item.path("description").asText(title);
            Instant publishedAt = parsePublicationDate(item.path("publication_date").asText(null));

            results.add(new RawDiscoveredJob(
                    id,
                    JobPlatform.REMOTIVE,
                    company,
                    title,
                    description,
                    location,
                    WorkModality.REMOTO,
                    null,
                    salary,
                    List.of(),
                    url,
                    publishedAt
            ));
        }
        return results;
    }

    private Instant parsePublicationDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
