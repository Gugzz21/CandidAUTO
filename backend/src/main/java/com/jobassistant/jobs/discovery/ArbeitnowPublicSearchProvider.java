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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Provedor de vagas abertas da API pública oficial da Arbeitnow (sem autenticação),
 * registrando URL, código HTTP e resultado de parsing.
 */
@Component
public class ArbeitnowPublicSearchProvider implements QueryJobSearchProvider {

    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final HttpClient httpClient;

    public ArbeitnowPublicSearchProvider(
            ObjectMapper objectMapper,
            @Value("${app.connectors.arbeitnow.api-url:https://www.arbeitnow.com/api/job-board-api}") String apiUrl
    ) {
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String providerName() {
        return "Arbeitnow Job Board API";
    }

    @Override
    public ProviderSearchResult search(DiscoveryCriteria criteria) {
        List<String> warnings = new ArrayList<>();
        ConnectorFetchResult res = fetchArbeitnowWithDiagnostics();
        if (res.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                || res.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                || res.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR) {
            warnings.add("Arbeitnow: " + res.diagnosticNote());
        }
        return new ProviderSearchResult(
                providerName(),
                1,
                List.of(new ProviderQueryExecution("Arbeitnow Public API", res)),
                warnings
        );
    }

    public ConnectorFetchResult fetchArbeitnowWithDiagnostics() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("User-Agent", "CandidAUTO/1.0 (+Official-Arbeitnow-API)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new ConnectorFetchResult(
                        List.of(),
                        false,
                        SyncOutcomeStatus.HTTP_ERROR,
                        apiUrl,
                        response.statusCode(),
                        "API Arbeitnow retornou HTTP " + response.statusCode()
                );
            }

            List<RawDiscoveredJob> parsed = parseArbeitnowJson(response.body());
            SyncOutcomeStatus outcome = parsed.isEmpty()
                    ? SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED
                    : SyncOutcomeStatus.SUCCESS_WITH_RESULTS;
            return new ConnectorFetchResult(parsed, true, outcome, apiUrl, response.statusCode(), "OK");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.NETWORK_ERROR, apiUrl, null, "Consulta interrompida");
        } catch (IOException e) {
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.NETWORK_ERROR, apiUrl, null, e.getMessage());
        } catch (IllegalArgumentException e) {
            return new ConnectorFetchResult(List.of(), false, SyncOutcomeStatus.PARSE_ERROR, apiUrl, null, e.getMessage());
        }
    }

    public List<RawDiscoveredJob> parseArbeitnowJson(String jsonBody) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            throw new IOException("JSON sem array 'data' esperado na API Arbeitnow");
        }

        List<RawDiscoveredJob> results = new ArrayList<>();
        for (JsonNode item : dataNode) {
            String slug = item.path("slug").asText("");
            String title = item.path("title").asText("").trim();
            if (title.isEmpty()) {
                continue;
            }
            String company = item.path("company_name").asText("Empresa Tech").trim();
            String location = item.path("location").asText("").trim();
            boolean remote = item.path("remote").asBoolean(false);
            WorkModality modality = remote ? WorkModality.REMOTO : null;
            String url = item.path("url").asText("").trim();
            String description = item.path("description").asText(title);
            long createdEpochSec = item.path("created_at").asLong(0L);
            Instant publishedAt = createdEpochSec > 0 ? Instant.ofEpochSecond(createdEpochSec) : null;

            results.add(new RawDiscoveredJob(
                    slug,
                    JobPlatform.ARBEITNOW,
                    company,
                    title,
                    description,
                    location,
                    modality,
                    null,
                    "",
                    List.of(),
                    url,
                    publishedAt
            ));
        }
        return results;
    }
}
