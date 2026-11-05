package com.jobassistant.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.dto.CreateJobSourceRequestDto;
import com.jobassistant.jobs.dto.ImportJobRequestDto;
import com.jobassistant.shared.PostgresUnixSocketBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresUnixSocketBridge.class)
class JobDiscoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterSourceImportJobsDeduplicateAndToggleAvailability() throws Exception {
        String userA = UUID.randomUUID().toString();
        String userB = UUID.randomUUID().toString();

        // 1. Cadastrar uma fonte de vagas da Greenhouse
        CreateJobSourceRequestDto sourceReq = new CreateJobSourceRequestDto(
                "Nubank Careers",
                JobPlatform.GREENHOUSE,
                "nubank"
        );

        mockMvc.perform(post("/api/v1/job-sources")
                        .header("X-User-Id", userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sourceReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nubank Careers"))
                .andExpect(jsonPath("$.platform").value("GREENHOUSE"));

        // 2. Importar uma vaga real com descrição completa e verificar extração e deduplicação
        ImportJobRequestDto importReq = new ImportJobRequestDto(
                JobPlatform.GUPY,
                "Itaú Unibanco",
                "Desenvolvedor Full Stack Júnior",
                "São Paulo, SP",
                WorkModality.HIBRIDO,
                "Júnior",
                "R$ 6.200",
                "https://itau.gupy.io/jobs/12345",
                """
                Requisitos Obrigatórios:
                • Experiência com Java e Spring Boot
                • Conhecimento de React e TypeScript
                Diferenciais Desejáveis:
                • Conhecimento de AWS e Docker
                Benefícios: Vale Refeição, Plano de Saúde, PLR.
                """
        );

        MvcResult firstImport = mockMvc.perform(post("/api/v1/jobs/import")
                        .header("X-User-Id", userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Itaú Unibanco"))
                .andExpect(jsonPath("$.workModality").value("HIBRIDO"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        String firstJobId = objectMapper.readTree(firstImport.getResponse().getContentAsString()).get("id").asText();

        // Importar exatamente a mesma vaga novamente — não deve duplicar (deve manter o mesmo ID)
        MvcResult secondImport = mockMvc.perform(post("/api/v1/jobs/import")
                        .header("X-User-Id", userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String secondJobId = objectMapper.readTree(secondImport.getResponse().getContentAsString()).get("id").asText();
        assertThat(secondJobId).isEqualTo(firstJobId);

        // 3. Listar vagas do usuário A e verificar que há apenas 1 registro
        MvcResult listRes = mockMvc.perform(get("/api/v1/jobs")
                        .header("X-User-Id", userA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jobsArray = objectMapper.readTree(listRes.getResponse().getContentAsString());
        assertThat(jobsArray).hasSize(1);

        // 4. Marcar vaga como expirada/indisponível
        mockMvc.perform(patch("/api/v1/jobs/" + firstJobId + "/availability")
                        .header("X-User-Id", userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 5. Usuário B não deve enxergar vagas do Usuário A
        MvcResult listUserB = mockMvc.perform(get("/api/v1/jobs")
                        .header("X-User-Id", userB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(listUserB.getResponse().getContentAsString())).isEmpty();
    }
}
