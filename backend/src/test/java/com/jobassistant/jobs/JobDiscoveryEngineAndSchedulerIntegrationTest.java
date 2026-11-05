package com.jobassistant.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.automation.domain.SearchConfigEntity;
import com.jobassistant.automation.domain.SearchConfigRepository;
import com.jobassistant.automation.dto.UpdateSearchConfigRequestDto;
import com.jobassistant.automation.service.JobMonitoringScheduler;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.dto.CreateJobSourceRequestDto;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.dto.UpdateProfileRequestDto;
import com.jobassistant.shared.PostgresUnixSocketBridge;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.discovery.open-web-enabled=true",
        "app.security.allow-test-user-header=true"
})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresUnixSocketBridge.class)
class JobDiscoveryEngineAndSchedulerIntegrationTest {

    private static HttpServer mockExternalInternet;
    private static int serverPort;
    private static final AtomicInteger careerPageMode = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobMatchAnalysisRepository matchAnalysisRepository;

    @Autowired
    private SearchConfigRepository searchConfigRepository;

    @Autowired
    private JobMonitoringScheduler monitoringScheduler;

    @BeforeAll
    static void startMockExternalJobApis() throws IOException {
        mockExternalInternet = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = mockExternalInternet.getAddress().getPort();

        // 1. Gupy Portal API pública (/gupy/jobs?jobName=...)
        mockExternalInternet.createContext("/gupy/jobs", exchange -> {
            String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";
            String body;
            if (query.toLowerCase().contains("java")) {
                body = """
                        {
                          "data": [
                            {
                              "id": 501,
                              "name": "Pessoa Desenvolvedora Java Júnior",
                              "careerPageName": "Itaú Unibanco",
                              "description": "Atuação com Java, Spring Boot, PostgreSQL e APIs REST.",
                              "city": "São Paulo",
                              "state": "SP",
                              "country": "Brasil",
                              "isRemoteWork": true,
                              "workplaceType": "remote",
                              "jobUrl": "https://itau.gupy.io/jobs/501",
                              "publishedDate": "2026-11-01T12:00:00Z"
                            },
                            {
                              "id": 502,
                              "name": "Engenheiro de Software Java Sênior",
                              "careerPageName": "Banco Alpha",
                              "description": "Vaga sênior com 8 anos de experiência.",
                              "city": "São Paulo",
                              "state": "SP",
                              "isRemoteWork": true,
                              "workplaceType": "remote",
                              "jobUrl": "https://alpha.gupy.io/jobs/502"
                            },
                            {
                              "id": 503,
                              "name": "Analista de Recursos Humanos",
                              "careerPageName": "Corp RH",
                              "description": "Recrutamento e seleção.",
                              "city": "São Paulo",
                              "state": "SP",
                              "isRemoteWork": true,
                              "jobUrl": "https://corprh.gupy.io/jobs/503"
                            }
                          ]
                        }
                        """;
            } else {
                body = """
                        { "data": [] }
                        """;
            }
            respondJson(exchange, 200, body);
        });

        // 2. Remotive Public API (/remotive/remote-jobs?search=...)
        mockExternalInternet.createContext("/remotive/remote-jobs", exchange -> {
            String query = exchange.getRequestURI().getQuery() != null ? exchange.getRequestURI().getQuery() : "";
            String body;
            if (query.toLowerCase().contains("frontend")) {
                body = """
                        {
                          "jobs": [
                            {
                              "id": 701,
                              "title": "Junior Frontend Developer",
                              "company_name": "Vercel",
                              "candidate_required_location": "Worldwide",
                              "salary": "USD 4.500",
                              "url": "https://remotive.com/jobs/701",
                              "publication_date": "2026-11-02T10:00:00",
                              "description": "<p>React, TypeScript, CSS and HTML.</p>"
                            }
                          ]
                        }
                        """;
            } else {
                body = """
                        { "jobs": [] }
                        """;
            }
            respondJson(exchange, 200, body);
        });

        // 3. Arbeitnow API (/arbeitnow/jobs) -> retorna apenas vaga de Vendas irrelevante
        mockExternalInternet.createContext("/arbeitnow/jobs", exchange -> {
            String body = """
                    {
                      "data": [
                        {
                          "slug": "sales-account-exec-1",
                          "company_name": "SalesForce Partner",
                          "title": "Account Executive",
                          "description": "B2B cold calling and enterprise sales.",
                          "remote": true,
                          "location": "Remote",
                          "url": "https://arbeitnow.com/jobs/sales-1",
                          "created_at": 1762000000
                        }
                      ]
                    }
                    """;
            respondJson(exchange, 200, body);
        });

        // 4. Greenhouse API oficial (/greenhouse/nubank/jobs)
        mockExternalInternet.createContext("/greenhouse/nubank/jobs", exchange -> {
            String body = """
                    {
                      "jobs": [
                        {
                          "id": 801,
                          "title": "Software Engineer - Backend",
                          "updated_at": "2026-11-03T10:00:00-03:00",
                          "location": { "name": "São Paulo, Brazil (Hybrid)" },
                          "absolute_url": "https://boards.greenhouse.io/nubank/jobs/801",
                          "content": "<p>Desenvolvimento back-end com Java, Spring Boot e microsserviços.</p>"
                        },
                        {
                          "id": 802,
                          "title": "Legal Counsel",
                          "updated_at": "2026-11-03T10:00:00-03:00",
                          "location": { "name": "São Paulo, Brazil" },
                          "absolute_url": "https://boards.greenhouse.io/nubank/jobs/802",
                          "content": "<p>Direito regulatório bancário.</p>"
                        }
                      ]
                    }
                    """;
            respondJson(exchange, 200, body);
        });

        // 5. Lever API oficial (/lever/avenue)
        mockExternalInternet.createContext("/lever/avenue", exchange -> {
            String body = """
                    [
                      {
                        "id": "lev-901",
                        "text": "Frontend Developer Jr",
                        "createdAt": 1762300000000,
                        "workplaceType": "remote",
                        "categories": { "location": "Remoto - Brasil" },
                        "descriptionPlain": "Desenvolvimento front-end com React e TypeScript.",
                        "hostedUrl": "https://jobs.lever.co/avenue/lev-901"
                      }
                    ]
                    """;
            respondJson(exchange, 200, body);
        });

        // 6. Career Page com mudança de estado para testar proteção contra expiração indevida
        mockExternalInternet.createContext("/career-page", exchange -> {
            int mode = careerPageMode.get();
            if (mode == 1) {
                String html = """
                        <html><head>
                        <script type="application/ld+json">
                        {
                          "@context": "https://schema.org",
                          "@type": "JobPosting",
                          "title": "Desenvolvedor Java Júnior",
                          "description": "Java, Spring Boot e SQL.",
                          "hiringOrganization": { "@type": "Organization", "name": "CloudNova" },
                          "jobLocationType": "TELECOMMUTE",
                          "url": "https://cloudnova.tech/jobs/1"
                        }
                        </script>
                        </head><body></body></html>
                        """;
                respondHtml(exchange, 200, html);
            } else if (mode == 2) {
                // Simula página bloqueada por anti-bot / JS dinâmico retornando 200 sem JSON-LD
                respondHtml(exchange, 200, "<html><body>Please enable JavaScript to view jobs.</body></html>");
            } else if (mode == 4) {
                // Simula JSON-LD malformado/corrompido para validar diagnóstico PARSE_ERROR explícito
                respondHtml(exchange, 200, "<html><head><script type=\"application/ld+json\">{ malformed_json: [ </script></head><body></body></html>");
            } else {
                respondHtml(exchange, 503, "Service Temporarily Unavailable");
            }
        });

        mockExternalInternet.start();
    }

    @AfterAll
    static void stopMockExternalJobApis() {
        if (mockExternalInternet != null) {
            mockExternalInternet.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerDynamicConnectorUrls(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + serverPort;
        registry.add("app.connectors.gupy.portal-api-url", () -> base + "/gupy/jobs");
        registry.add("app.connectors.remotive.api-url", () -> base + "/remotive/remote-jobs");
        registry.add("app.connectors.arbeitnow.api-url", () -> base + "/arbeitnow/jobs");
        registry.add("app.connectors.greenhouse.base-url", () -> base + "/greenhouse");
        registry.add("app.connectors.lever.base-url", () -> base + "/lever");
        registry.add("app.discovery.curated-greenhouse-boards", () -> "nubank:Nubank");
        registry.add("app.discovery.curated-lever-boards", () -> "avenue:Avenue");
    }

    @Test
    void shouldDiscoverJobsAutomaticallyWithoutManualSourcesFilterIrrelevantAndExecutePeriodicSchedulerPerUser() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        // 1. Configurar e confirmar perfil do Usuário A
        UpdateProfileRequestDto profileA = new UpdateProfileRequestDto(
                "Mariana Costa",
                "Desenvolvedora Java & Front-end Júnior",
                "Júnior",
                "Desenvolvedora Júnior com foco em Java, Spring Boot, React e TypeScript.",
                new ContactInfoDto("mariana@dev.br", "11999990000", "São Paulo, SP", "", "", ""),
                List.of(),
                List.of(),
                List.of(
                        new SkillItemDto("Java", "LANGUAGE_TECH", "Intermediário", "Java"),
                        new SkillItemDto("Spring Boot", "LANGUAGE_TECH", "Intermediário", "Spring Boot"),
                        new SkillItemDto("React", "LANGUAGE_TECH", "Intermediário", "React"),
                        new SkillItemDto("TypeScript", "LANGUAGE_TECH", "Intermediário", "TypeScript")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of("Desenvolvedor Java Júnior", "Desenvolvedor Front-end Júnior")
        );

        mockMvc.perform(put("/api/v1/profile")
                        .header("X-User-Id", userA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileA)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/profile/confirm")
                        .header("X-User-Id", userA.toString()))
                .andExpect(status().isOk());

        // Configurar automação do Usuário A (habilitada, intervalo 60 min) e Usuário B (desabilitada)
        UpdateSearchConfigRequestDto configA = new UpdateSearchConfigRequestDto(
                List.of("Desenvolvedor Java Júnior", "Desenvolvedor Front-end Júnior"),
                List.of("REMOTO", "HIBRIDO"),
                List.of("Júnior"),
                60,
                true,
                60
        );
        mockMvc.perform(put("/api/v1/search-config")
                        .header("X-User-Id", userA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configA)))
                .andExpect(status().isOk());

        UpdateSearchConfigRequestDto configB = new UpdateSearchConfigRequestDto(
                List.of("Desenvolvedor Java Júnior"),
                List.of("REMOTO"),
                List.of("Júnior"),
                70,
                false,
                60
        );
        mockMvc.perform(put("/api/v1/search-config")
                        .header("X-User-Id", userB.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configB)))
                .andExpect(status().isOk());

        // 2. Simular execução do Agendador Periódico SEM contexto HTTP (exatamente como @Scheduled roda em background!)
        RequestContextHolder.resetRequestAttributes();
        Instant t0 = Instant.now();
        monitoringScheduler.checkAndRunDueMonitoringCycles(t0);

        // Verificar que o Usuário A recebeu automaticamente as 4 vagas relevantes (Gupy, Remotive, Greenhouse, Lever)
        // mesmo sem ter cadastrado NENHUMA fonte manual!
        List<JobPostingEntity> jobsUserA = jobPostingRepository.findAllByUserIdOrderByDiscoveredAtDesc(userA);
        assertThat(jobsUserA).hasSize(4);
        assertThat(jobsUserA)
                .extracting(JobPostingEntity::getCompanyName)
                .containsExactlyInAnyOrder("Itaú Unibanco", "Vercel", "Nubank", "Avenue");

        // Verificar que vagas Sênior, RH, Vendas e Jurídico foram filtradas e NÃO entraram no banco
        assertThat(jobsUserA)
                .extracting(JobPostingEntity::getTitle)
                .doesNotContain("Engenheiro de Software Java Sênior", "Analista de Recursos Humanos", "Account Executive", "Legal Counsel");

        // Verificar que o AI Matching Engine analisou as 4 vagas automaticamente para o Usuário A
        assertThat(matchAnalysisRepository.findAllByUserIdOrderByCompatibilityScoreDesc(userA)).hasSize(4);

        // Verificar Telemetria Diagnóstica Reproduzível (GET /api/v1/jobs/diagnostics):
        // URL consultada, código HTTP, presença de vagas, extraídas pelo parser, descartadas com motivo e inseridas!
        MvcResult diagRes = mockMvc.perform(get("/api/v1/jobs/diagnostics")
                        .header("X-User-Id", userA.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode diagArray = objectMapper.readTree(diagRes.getResponse().getContentAsString());
        assertThat(diagArray.isArray()).isTrue();
        assertThat(diagArray).isNotEmpty();

        boolean foundGupyJavaDiagnosticWithDiscardReasons = false;
        for (JsonNode diag : diagArray) {
            assertThat(diag.get("queriedUrl").asText()).startsWith("http://127.0.0.1:" + serverPort);
            assertThat(diag.get("httpStatus").asInt()).isEqualTo(200);
            if (diag.get("connectorName").asText().startsWith("Gupy (")
                    && diag.get("parsedCount").asInt() == 3) {
                foundGupyJavaDiagnosticWithDiscardReasons = true;
                assertThat(diag.get("responseContainedJobs").asBoolean()).isTrue();
                assertThat(diag.get("discardedCount").asInt()).isEqualTo(2);
                assertThat(diag.get("savedNewCount").asInt()).isEqualTo(1);
                assertThat(diag.get("discardReasons").has("SENIORIDADE_INCOMPATIVEL")).isTrue();
                assertThat(diag.get("discardReasons").has("AREA_ALHEIA_AO_CARGO")).isTrue();
            }
        }
        assertThat(foundGupyJavaDiagnosticWithDiscardReasons).isTrue();

        // Verificar isolamento SaaS: Usuário B (com automação desabilitada) tem 0 vagas
        assertThat(jobPostingRepository.findAllByUserIdOrderByDiscoveredAtDesc(userB)).isEmpty();

        // 3. Verificar respeito ao intervalo configurado: 10 minutos depois (< 60 min), o agendador NÃO deve reexecutar userA
        Instant lastRunAfterFirstTick = searchConfigRepository.findByUserId(userA).orElseThrow().getLastRunAt();
        assertThat(lastRunAfterFirstTick).isNotNull();

        monitoringScheduler.checkAndRunDueMonitoringCycles(lastRunAfterFirstTick.plus(Duration.ofMinutes(10)));
        Instant lastRunAfter10Min = searchConfigRepository.findByUserId(userA).orElseThrow().getLastRunAt();
        assertThat(lastRunAfter10Min).isEqualTo(lastRunAfterFirstTick);

        // 4. Após 61 minutos (>= 60 min configurados), o agendador DEVE executar novo ciclo automaticamente!
        monitoringScheduler.checkAndRunDueMonitoringCycles(lastRunAfterFirstTick.plus(Duration.ofMinutes(61)));
        SearchConfigEntity updatedConfigA = searchConfigRepository.findByUserId(userA).orElseThrow();
        assertThat(updatedConfigA.getLastRunAt()).isAfter(lastRunAfterFirstTick);
    }

    @Test
    void shouldProtectActiveJobsAgainstPrematureExpirationAndRejectUnsupportedBoardPlatforms() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Tentar cadastrar LINKEDIN como conector automático de board deve retornar 400 Bad Request claro
        CreateJobSourceRequestDto invalidLinkedinBoard = new CreateJobSourceRequestDto(
                "LinkedIn Geral",
                JobPlatform.LINKEDIN,
                "linkedin"
        );
        mockMvc.perform(post("/api/v1/job-sources")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLinkedinBoard)))
                .andExpect(status().isBadRequest());

        // 2. Cadastrar Career Page válida e sincronizar (Modo 1: retorna 1 vaga estruturada)
        careerPageMode.set(1);
        CreateJobSourceRequestDto careerSourceReq = new CreateJobSourceRequestDto(
                "CloudNova Careers",
                JobPlatform.CAREER_PAGE,
                "http://127.0.0.1:" + serverPort + "/career-page"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/job-sources")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(careerSourceReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String sourceId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/job-sources/" + sourceId + "/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());

        List<JobPostingEntity> jobsAfterFirstSync = jobPostingRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId);
        assertThat(jobsAfterFirstSync).hasSize(1);
        assertThat(jobsAfterFirstSync.get(0).isActive()).isTrue();

        // 3. Modo 2: Página retorna HTTP 200 sem JSON-LD (ex.: bloqueio anti-bot ou renderização dinâmica vazia)
        // A vaga anterior NÃO pode ser marcada como expirada!
        careerPageMode.set(2);
        MvcResult emptyHtmlSync = mockMvc.perform(post("/api/v1/job-sources/" + sourceId + "/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode syncJson = objectMapper.readTree(emptyHtmlSync.getResponse().getContentAsString());
        assertThat(syncJson.get("markedExpired").asInt()).isEqualTo(0);
        assertThat(syncJson.get("source").get("lastSyncStatus").asText()).contains("Aviso: 0 vagas extraídas");

        List<JobPostingEntity> jobsAfterAmbiguousEmpty = jobPostingRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId);
        assertThat(jobsAfterAmbiguousEmpty).hasSize(1);
        assertThat(jobsAfterAmbiguousEmpty.get(0).isActive()).isTrue();

        // 4. Modo 4: JSON-LD malformado reporta PARSE_ERROR explícito (não oculta erro de parsing) e preserva vaga ativa
        careerPageMode.set(4);
        MvcResult parseErrorSync = mockMvc.perform(post("/api/v1/job-sources/" + sourceId + "/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode parseErrorJson = objectMapper.readTree(parseErrorSync.getResponse().getContentAsString());
        assertThat(parseErrorJson.get("markedExpired").asInt()).isEqualTo(0);
        assertThat(parseErrorJson.get("diagnostic").get("outcomeStatus").asText()).isEqualTo("PARSE_ERROR");
        assertThat(parseErrorJson.get("source").get("lastSyncStatus").asText()).contains("Falha de parsing");

        // 5. Modo 3: Falha HTTP 503 na página externa retorna 422 controlado, registra falha na fonte e mantém vaga ativa
        careerPageMode.set(3);
        mockMvc.perform(post("/api/v1/job-sources/" + sourceId + "/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isUnprocessableEntity());

        List<JobPostingEntity> jobsAfterHttpFailure = jobPostingRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId);
        assertThat(jobsAfterHttpFailure).hasSize(1);
        assertThat(jobsAfterHttpFailure.get(0).isActive()).isTrue();
    }

    private static void respondJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void respondHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
