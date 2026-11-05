package com.jobassistant.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.connector.CareerPageJsonLdConnector;
import com.jobassistant.jobs.connector.GreenhouseJobConnector;
import com.jobassistant.jobs.connector.LeverJobConnector;
import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.service.JobNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobConnectorsAndNormalizationTest {

    private ObjectMapper objectMapper;
    private GreenhouseJobConnector greenhouseConnector;
    private LeverJobConnector leverConnector;
    private CareerPageJsonLdConnector careerPageConnector;
    private JobNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        greenhouseConnector = new GreenhouseJobConnector(objectMapper, "https://boards-api.greenhouse.io/v1/boards");
        leverConnector = new LeverJobConnector(objectMapper, "https://api.lever.co/v0/postings");
        careerPageConnector = new CareerPageJsonLdConnector(objectMapper);
        normalizationService = new JobNormalizationService();
    }

    @Test
    void shouldParseGreenhouseJobsAndApplicationQuestions() throws IOException {
        String ghJson = """
                {
                  "jobs": [
                    {
                      "id": 9001,
                      "title": "Desenvolvedor Full Stack Júnior (Remoto)",
                      "updated_at": "2026-11-05T10:00:00-03:00",
                      "location": { "name": "São Paulo, Brasil - Remoto" },
                      "absolute_url": "https://boards.greenhouse.io/acme/jobs/9001",
                      "content": "&lt;h3&gt;Requisitos Obrigatórios&lt;/h3&gt;&lt;ul&gt;&lt;li&gt;Experiência com Java e Spring Boot&lt;/li&gt;&lt;li&gt;Conhecimento em React e TypeScript&lt;/li&gt;&lt;/ul&gt;&lt;h3&gt;Diferenciais Desejáveis&lt;/h3&gt;&lt;ul&gt;&lt;li&gt;Conhecimento básico de AWS e Docker&lt;/li&gt;&lt;/ul&gt;&lt;p&gt;Salário: R$ 5.500 - R$ 6.800. Benefícios: Vale Refeição, Plano de Saúde, Gympass.&lt;/p&gt;",
                      "questions": [
                        {
                          "label": "Qual sua pretensão salarial?",
                          "required": true,
                          "fields": [{ "name": "question_salary", "type": "input_text" }]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<RawDiscoveredJob> rawJobs = greenhouseConnector.parseGreenhouseJobsJson(ghJson, "Acme Tech");
        assertThat(rawJobs).hasSize(1);

        JobPostingEntity normalized = normalizationService.normalizeToEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                rawJobs.get(0)
        );

        assertThat(normalized.getCompanyName()).isEqualTo("Acme Tech");
        assertThat(normalized.getTitle()).isEqualTo("Desenvolvedor Full Stack Júnior (Remoto)");
        assertThat(normalized.getSeniority()).isEqualTo("Júnior");
        assertThat(normalized.getWorkModality()).isEqualTo(WorkModality.REMOTO);
        assertThat(normalized.getSalaryRange()).contains("R$ 5.500");
        assertThat(normalized.getTechnologies()).contains("Java", "Spring Boot", "React", "TypeScript", "AWS", "Docker");
        assertThat(normalized.getMandatoryRequirements()).anyMatch(r -> r.contains("Java e Spring Boot"));
        assertThat(normalized.getDesirableRequirements()).anyMatch(r -> r.contains("AWS e Docker"));
        assertThat(normalized.getBenefits()).contains("Vale Refeição", "Plano de Saúde", "Gympass");
        assertThat(normalized.getApplicationQuestions()).hasSize(1);
    }

    @Test
    void shouldParseLeverPostingsJson() throws IOException {
        String leverJson = """
                [
                  {
                    "id": "lev-123",
                    "text": "Software Engineer Pleno - Back-end",
                    "createdAt": 1762340000000,
                    "workplaceType": "hybrid",
                    "categories": { "location": "Curitiba, PR" },
                    "descriptionPlain": "Buscamos pessoa desenvolvedora para atuar com Java, PostgreSQL e Kafka.",
                    "hostedUrl": "https://jobs.lever.co/fintech/lev-123",
                    "lists": [
                      {
                        "text": "Requisitos",
                        "content": "<li>Sólida experiência com Java e Spring Boot</li>"
                      }
                    ]
                  }
                ]
                """;

        List<RawDiscoveredJob> rawJobs = leverConnector.parseLeverPostingsJson(leverJson, "Fintech Brasil");
        assertThat(rawJobs).hasSize(1);

        JobPostingEntity normalized = normalizationService.normalizeToEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                rawJobs.get(0)
        );

        assertThat(normalized.getWorkModality()).isEqualTo(WorkModality.HIBRIDO);
        assertThat(normalized.getSeniority()).isEqualTo("Pleno");
        assertThat(normalized.getTechnologies()).contains("Java", "PostgreSQL", "Kafka", "Spring Boot");
    }

    @Test
    void shouldParseCareerPageSchemaOrgJsonLdAndGupyNextData() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                      {
                        "@context": "https://schema.org",
                        "@type": "JobPosting",
                        "title": "Engenheiro de Software Júnior",
                        "description": "<p>Desenvolvimento com TypeScript, Node.js, React e PostgreSQL.</p>",
                        "hiringOrganization": { "@type": "Organization", "name": "CloudNova" },
                        "jobLocationType": "TELECOMMUTE",
                        "jobLocation": {
                          "address": { "addressLocality": "São Paulo", "addressRegion": "SP" }
                        },
                        "baseSalary": {
                          "currency": "BRL",
                          "value": { "minValue": 4500, "maxValue": 6000 }
                        },
                        "url": "https://carreiras.cloudnova.com.br/vagas/eng-jr"
                      }
                    </script>
                  </head>
                  <body></body>
                </html>
                """;

        List<RawDiscoveredJob> jobs = careerPageConnector.parseHtmlForStructuredJobs(
                html,
                JobPlatform.CAREER_PAGE,
                "CloudNova",
                "https://carreiras.cloudnova.com.br"
        );

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Engenheiro de Software Júnior");
        assertThat(jobs.get(0).explicitModality()).isEqualTo(WorkModality.REMOTO);
        assertThat(jobs.get(0).salaryRange()).isEqualTo("BRL 4500 - 6000");
    }
}
