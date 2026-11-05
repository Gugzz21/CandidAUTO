package com.jobassistant.jobs.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.FileProcessingException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conector para páginas oficiais de carreiras com dados estruturados (Schema.org JobPosting JSON-LD e __NEXT_DATA__).
 * Registra código HTTP, URL consultada e erros de interpretação (sem ocultar falhas de parsing).
 */
@Component
public class CareerPageJsonLdConnector implements JobSourceConnector {

    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
            "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern NEXT_DATA_SCRIPT = Pattern.compile(
            "<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CareerPageJsonLdConnector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean supports(JobPlatform platform) {
        return platform == JobPlatform.CAREER_PAGE;
    }

    @Override
    public List<RawDiscoveredJob> fetchJobs(JobSourceEntity source) {
        return fetchJobsSafely(source).jobs();
    }

    @Override
    public ConnectorFetchResult fetchJobsSafely(JobSourceEntity source) {
        String targetUrl = resolveUrl(source);
        return fetchStructuredResultFromUrl(targetUrl, source.getPlatform(), source.getName());
    }

    public List<RawDiscoveredJob> fetchStructuredJobsFromUrl(
            String targetUrl,
            JobPlatform platform,
            String defaultCompany
    ) {
        return fetchStructuredResultFromUrl(targetUrl, platform, defaultCompany).jobs();
    }

    public ConnectorFetchResult fetchStructuredResultFromUrl(
            String targetUrl,
            JobPlatform platform,
            String defaultCompany
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "text/html,application/xhtml+xml,application/json")
                    .header("User-Agent", "CandidAUTO/1.0 (+Structured-CareerPage-Reader)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException("A página de carreiras (" + targetUrl + ") retornou HTTP " + response.statusCode() + ".");
            }

            StructuredHtmlParseReport report = inspectAndParseHtml(response.body(), platform, defaultCompany, targetUrl);
            if (!report.jobs().isEmpty()) {
                return new ConnectorFetchResult(
                        report.jobs(),
                        false,
                        SyncOutcomeStatus.SUCCESS_WITH_RESULTS,
                        targetUrl,
                        response.statusCode(),
                        "Extraídas " + report.jobs().size() + " vaga(s) estruturada(s)"
                );
            }

            if (report.parseErrorsCount() > 0) {
                return new ConnectorFetchResult(
                        List.of(),
                        false,
                        SyncOutcomeStatus.PARSE_ERROR,
                        targetUrl,
                        response.statusCode(),
                        "Falha ao interpretar " + report.parseErrorsCount() + " bloco(s) JSON estruturado(s) na página (" + report.lastParseError() + ")"
                );
            }

            return new ConnectorFetchResult(
                    List.of(),
                    false,
                    SyncOutcomeStatus.EXTRACTION_INCONCLUSIVE,
                    targetUrl,
                    response.statusCode(),
                    "HTTP " + response.statusCode() + " recebido, mas sem blocos Schema.org JobPosting ou __NEXT_DATA__ (vagas anteriores preservadas)"
            );
        } catch (BadRequestException | FileProcessingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("Leitura da página de carreiras interrompida.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new FileProcessingException("Não foi possível consultar a página de carreiras: " + targetUrl, e);
        }
    }

    public List<RawDiscoveredJob> parseHtmlForStructuredJobs(
            String html,
            JobPlatform platform,
            String defaultCompany,
            String pageUrl
    ) {
        return inspectAndParseHtml(html, platform, defaultCompany, pageUrl).jobs();
    }

    public StructuredHtmlParseReport inspectAndParseHtml(
            String html,
            JobPlatform platform,
            String defaultCompany,
            String pageUrl
    ) {
        List<RawDiscoveredJob> discovered = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return new StructuredHtmlParseReport(discovered, 0, "");
        }

        int parseErrors = 0;
        String lastError = "";

        // 1. Extrair dados estruturados Schema.org JobPosting (application/ld+json)
        Matcher ldMatcher = JSON_LD_SCRIPT.matcher(html);
        while (ldMatcher.find()) {
            String jsonContent = ldMatcher.group(1).trim();
            try {
                JsonNode node = objectMapper.readTree(jsonContent);
                collectJobPostingsFromJsonLd(node, platform, defaultCompany, pageUrl, discovered);
            } catch (IOException ex) {
                parseErrors++;
                lastError = "JSON-LD malformado: " + ex.getMessage();
            }
        }

        // 2. Extrair dados estruturados públicos __NEXT_DATA__ (portais Next.js / Gupy)
        Matcher nextMatcher = NEXT_DATA_SCRIPT.matcher(html);
        while (nextMatcher.find()) {
            String jsonContent = nextMatcher.group(1).trim();
            try {
                JsonNode root = objectMapper.readTree(jsonContent);
                JsonNode pageProps = root.path("props").path("pageProps");
                JsonNode jobsArray = pageProps.path("jobs");
                if (!jobsArray.isArray()) {
                    jobsArray = pageProps.path("initialState").path("jobs").path("list");
                }
                if (jobsArray.isArray()) {
                    for (JsonNode job : jobsArray) {
                        String id = job.path("id").asText("");
                        String title = firstNonBlank(job, "name", "title");
                        if (title.isEmpty()) continue;
                        String city = job.path("city").asText("");
                        String state = job.path("state").asText("");
                        String location = (city + (state.isBlank() ? "" : " - " + state)).trim();
                        String workplaceType = job.path("workplaceType").asText("").toLowerCase(Locale.ROOT);
                        boolean isRemoteFlag = job.path("isRemoteWork").asBoolean(false);
                        WorkModality modality = (isRemoteFlag || workplaceType.contains("remote")) ? WorkModality.REMOTO
                                : workplaceType.contains("hybrid") ? WorkModality.HIBRIDO
                                : workplaceType.contains("on-site") ? WorkModality.PRESENCIAL : null;
                        String jobUrl = firstNonBlank(job, "jobUrl", "url");
                        if (jobUrl.isBlank()) {
                            jobUrl = pageUrl;
                        }
                        String company = firstNonBlank(job, "careerPageName", "companyName");
                        if (company.isBlank()) {
                            company = defaultCompany;
                        }
                        String desc = job.path("description").asText("");
                        if (desc.isBlank()) {
                            desc = title + " - " + company;
                        }

                        discovered.add(new RawDiscoveredJob(
                                id,
                                platform,
                                company,
                                title,
                                desc,
                                location,
                                modality,
                                null,
                                "",
                                List.of(),
                                jobUrl,
                                Instant.now()
                        ));
                    }
                }
            } catch (IOException ex) {
                parseErrors++;
                lastError = "__NEXT_DATA__ malformado: " + ex.getMessage();
            }
        }

        return new StructuredHtmlParseReport(discovered, parseErrors, lastError);
    }

    private void collectJobPostingsFromJsonLd(
            JsonNode node,
            JobPlatform platform,
            String defaultCompany,
            String fallbackUrl,
            List<RawDiscoveredJob> out
    ) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectJobPostingsFromJsonLd(item, platform, defaultCompany, fallbackUrl, out);
            }
            return;
        }
        if (node.has("@graph") && node.get("@graph").isArray()) {
            collectJobPostingsFromJsonLd(node.get("@graph"), platform, defaultCompany, fallbackUrl, out);
        }

        String type = node.path("@type").asText("");
        if ("JobPosting".equalsIgnoreCase(type)) {
            String title = node.path("title").asText("").trim();
            if (title.isEmpty()) {
                return;
            }
            String externalId = node.path("identifier").path("value").asText(
                    node.path("identifier").asText("")
            );
            String company = node.path("hiringOrganization").path("name").asText(defaultCompany);
            if (company.isBlank()) {
                company = defaultCompany;
            }
            String description = node.path("description").asText(title);
            String location = node.path("jobLocation").path("address").path("addressLocality").asText("");
            String region = node.path("jobLocation").path("address").path("addressRegion").asText("");
            if (!region.isBlank()) {
                location = location.isBlank() ? region : location + ", " + region;
            }

            String locType = node.path("jobLocationType").asText("");
            WorkModality modality = "TELECOMMUTE".equalsIgnoreCase(locType) ? WorkModality.REMOTO : null;

            String salary = "";
            JsonNode baseSalary = node.path("baseSalary");
            if (!baseSalary.isMissingNode()) {
                String currency = baseSalary.path("currency").asText("BRL");
                JsonNode valNode = baseSalary.path("value");
                if (valNode.has("minValue") || valNode.has("maxValue")) {
                    salary = currency + " " + valNode.path("minValue").asText("") + " - " + valNode.path("maxValue").asText("");
                } else if (!valNode.asText("").isBlank()) {
                    salary = currency + " " + valNode.asText("");
                }
            }

            String url = node.path("url").asText(fallbackUrl);
            out.add(new RawDiscoveredJob(
                    externalId,
                    platform,
                    company,
                    title,
                    description,
                    location,
                    modality,
                    null,
                    salary,
                    List.of(),
                    url,
                    Instant.now()
            ));
        }
    }

    private String resolveUrl(JobSourceEntity source) {
        String id = source.getBoardIdentifier().trim();
        if (id.startsWith("http://") || id.startsWith("https://")) {
            return id;
        }
        throw new BadRequestException("Para páginas de carreiras, informe uma URL completa iniciando com https://");
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

    public record StructuredHtmlParseReport(
            List<RawDiscoveredJob> jobs,
            int parseErrorsCount,
            String lastParseError
    ) {
    }
}
