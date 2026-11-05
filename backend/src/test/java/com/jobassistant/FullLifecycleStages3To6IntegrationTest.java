package com.jobassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.applications.dto.AnswerMissingQuestionsRequestDto;
import com.jobassistant.applications.dto.CancelApplicationRequestDto;
import com.jobassistant.applications.dto.SubmitApplicationRequestDto;
import com.jobassistant.applications.dto.UpdateApplicationStatusRequestDto;
import com.jobassistant.automation.dto.UpdateSearchConfigRequestDto;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.dto.ImportJobRequestDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.dto.UpdateProfileRequestDto;
import com.jobassistant.shared.PostgresUnixSocketBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresUnixSocketBridge.class)
class FullLifecycleStages3To6IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExecuteMatchingSmartPreparationExplicitApprovalTrackingAndAutomation() throws Exception {
        String userId = UUID.randomUUID().toString();

        // 1. Criar e confirmar o Perfil Profissional do candidato
        UpdateProfileRequestDto profileReq = new UpdateProfileRequestDto(
                "Lucas Mendes",
                "Desenvolvedor Full Stack Júnior",
                "Júnior",
                "Desenvolvedor Full Stack com experiência prática em Java, Spring Boot, React, TypeScript e integração de APIs REST.",
                new ContactInfoDto(
                        "lucas.mendes@dev.com",
                        "(11) 99887-6655",
                        "São Paulo, SP",
                        "https://linkedin.com/in/lucasmendes",
                        "https://github.com/lucasmendes",
                        "https://lucasmendes.dev"
                ),
                List.of(),
                List.of(new EducationItemDto("USP", "Bacharelado em Sistemas de Informação", "Tecnologia", "2021", "2025", "Concluído", "USP - Sistemas de Informação")),
                List.of(
                        new SkillItemDto("Java", "LANGUAGE_TECH", "Intermediário", "Java e Spring Boot"),
                        new SkillItemDto("Spring Boot", "LANGUAGE_TECH", "Intermediário", "Java e Spring Boot"),
                        new SkillItemDto("React", "LANGUAGE_TECH", "Intermediário", "React e TypeScript"),
                        new SkillItemDto("TypeScript", "LANGUAGE_TECH", "Intermediário", "React e TypeScript")
                ),
                List.of(new LanguageItemDto("Inglês", "Intermediário", "Inglês Intermediário")),
                List.of(),
                List.of(),
                List.of("Desenvolvedor Full Stack Júnior", "Desenvolvedor Back-end Júnior")
        );

        mockMvc.perform(put("/api/v1/profile")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/profile/confirm")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));

        // 2. Importar duas vagas reais: uma altamente compatível (Júnior Full Stack com AWS desejável) e outra Sênior C#
        ImportJobRequestDto highMatchJobReq = new ImportJobRequestDto(
                JobPlatform.GREENHOUSE,
                "Fintech Brasil",
                "Desenvolvedor Full Stack Júnior",
                "São Paulo, SP",
                WorkModality.REMOTO,
                "Júnior",
                "R$ 6.000 - R$ 7.500",
                "https://boards.greenhouse.io/fintechbrasil/jobs/9001",
                """
                Requisitos Obrigatórios:
                • Experiência com React e TypeScript
                • Conhecimento de Java e Spring Boot
                Diferenciais Desejáveis:
                • Conhecimento de AWS e Docker
                Benefícios: Plano de Saúde, Vale Refeição, Auxílio Home Office.
                """
        );

        MvcResult job1Res = mockMvc.perform(post("/api/v1/jobs/import")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highMatchJobReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String job1Id = objectMapper.readTree(job1Res.getResponse().getContentAsString()).get("id").asText();

        ImportJobRequestDto lowMatchJobReq = new ImportJobRequestDto(
                JobPlatform.LINKEDIN,
                "Enterprise Corp",
                "Engenheiro .NET Sênior",
                "Curitiba, PR",
                WorkModality.PRESENCIAL,
                "Sênior",
                "R$ 16.000",
                "https://linkedin.com/jobs/view/9002",
                """
                Requisitos Obrigatórios:
                • Sólida experiência com C# e Kubernetes
                """
        );

        mockMvc.perform(post("/api/v1/jobs/import")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowMatchJobReq)))
                .andExpect(status().isCreated());

        // 3. Etapa 3: Analisar compatibilidade de todas as vagas ativas
        MvcResult matchAllRes = mockMvc.perform(post("/api/v1/jobs/match-all")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode matches = objectMapper.readTree(matchAllRes.getResponse().getContentAsString());
        assertThat(matches).hasSize(2);
        // O primeiro resultado deve ser a vaga de maior compatibilidade (Desenvolvedor Full Stack Júnior)
        int topScore = matches.get(0).get("compatibilityScore").asInt();
        int secondScore = matches.get(1).get("compatibilityScore").asInt();
        assertThat(topScore).isGreaterThan(secondScore);
        assertThat(topScore).isGreaterThanOrEqualTo(75);
        assertThat(matches.get(0).get("positivePoints")).isNotEmpty();
        assertThat(matches.get(0).get("attentionPoints")).isNotEmpty();

        // 4. Etapa 3: Consultar Painel de Oportunidades com filtros
        MvcResult oppRes = mockMvc.perform(get("/api/v1/opportunities")
                        .header("X-User-Id", userId)
                        .param("minScore", "70")
                        .param("modality", "REMOTO"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode filteredOpps = objectMapper.readTree(oppRes.getResponse().getContentAsString());
        assertThat(filteredOpps).hasSize(1);
        assertThat(filteredOpps.get(0).get("job").get("companyName").asText()).isEqualTo("Fintech Brasil");

        // 5. Etapa 4: Preenchimento inteligente da candidatura para job1
        MvcResult prepRes = mockMvc.perform(post("/api/v1/applications/prepare/" + job1Id)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_REVISAO"))
                .andExpect(jsonPath("$.submissionMode").value("INTEGRACAO_AUTORIZADA"))
                .andReturn();

        JsonNode prepJson = objectMapper.readTree(prepRes.getResponse().getContentAsString());
        String applicationId = prepJson.get("id").asText();
        assertThat(prepJson.get("preparedFields")).isNotEmpty();
        assertThat(prepJson.get("missingQuestions")).hasSize(2);

        // 6. Etapa 5 (Guardrail): Tentar enviar candidatura SEM responder perguntas obrigatórias ou SEM aprovação explícita deve falhar (400)
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitApplicationRequestDto(false, "Tentativa sem aprovação"))))
                .andExpect(status().isBadRequest());

        // 7. Etapa 4: Responder as perguntas pendentes e salvar no Cofre do Candidato (QA Vault)
        AnswerMissingQuestionsRequestDto answerReq = new AnswerMissingQuestionsRequestDto(
                Map.of(
                        "pretensao_salarial", "R$ 6.500 CLT",
                        "disponibilidade_inicio", "Imediata"
                ),
                true
        );

        mockMvc.perform(put("/api/v1/applications/" + applicationId + "/answers")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTA_PARA_CANDIDATURA"))
                .andExpect(jsonPath("$.missingQuestions").isEmpty());

        // Verificar que o Cofre de Respostas Confirmadas armazenou os itens
        mockMvc.perform(get("/api/v1/qa-vault")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 8. Etapa 5: Testar botão CANCELAR mantendo como rascunho
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/cancel")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelApplicationRequestDto(true, "Vou revisar mais tarde"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_REVISAO"))
                .andExpect(jsonPath("$.userApprovedExplicitly").value(false));

        // 9. Etapa 5: Confirmar explicitamente pelo BOTÃO CANDIDATAR-SE (explicitApprovalConfirmed = true)
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/submit")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitApplicationRequestDto(true, "Revisado e aprovado pelo candidato"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANDIDATURA_ENVIADA"))
                .andExpect(jsonPath("$.userApprovedExplicitly").value(true))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.submissionReceipt").isNotEmpty());

        // 10. Etapa 6: Atualizar status para ENTREVISTA_AGENDADA
        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateApplicationStatusRequestDto(
                                ApplicationStatus.ENTREVISTA_AGENDADA,
                                "Entrevista técnica agendada para sexta-feira às 14h"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREVISTA_AGENDADA"));

        // 11. Etapa 6: Configurar monitoramento automático e executar ciclo
        UpdateSearchConfigRequestDto configReq = new UpdateSearchConfigRequestDto(
                List.of("Desenvolvedor Front-end Júnior", "Desenvolvedor Back-end Júnior", "Desenvolvedor Full Stack Júnior"),
                List.of("REMOTO", "HIBRIDO"),
                List.of("Júnior"),
                70,
                true,
                60
        );

        mockMvc.perform(put("/api/v1/search-config")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minCompatibilityScore").value(70));

        mockMvc.perform(post("/api/v1/search-config/run-now")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastRunSummary").isNotEmpty());

        // 12. Etapa 6: Consultar indicadores (KPIs) do Dashboard Principal
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobsFound").value(2))
                .andExpect(jsonPath("$.jobsAnalyzedByAi").value(2))
                .andExpect(jsonPath("$.highCompatibilityJobs").value(1))
                .andExpect(jsonPath("$.applicationsSubmitted").value(1))
                .andExpect(jsonPath("$.interviewsScheduled").value(1))
                .andExpect(jsonPath("$.conversionRatePercent").value(50.0));
    }
}
