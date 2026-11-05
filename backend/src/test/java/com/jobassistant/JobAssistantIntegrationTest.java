package com.jobassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.dto.UpdateProfileRequestDto;
import com.jobassistant.resume.parser.ResumeTextExtractorTest;
import com.jobassistant.shared.PostgresUnixSocketBridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresUnixSocketBridge.class)
class JobAssistantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteFullFlowFromResumeUploadToProfileConfirmation() throws Exception {
        String userA = UUID.randomUUID().toString();

        byte[] pdfBytes = ResumeTextExtractorTest.createPdfWithText(
                "Rafael Almeida",
                "Desenvolvedor Full Stack Júnior",
                "rafael.almeida@dev.com | (11) 98765-4321 | São Paulo, SP",
                "linkedin.com/in/rafaelalmeida | github.com/rafaelalmeida",
                "Desenvolvedor Back-end - TechCorp | 2024 - Atual",
                "Desenvolvimento de APIs REST com Java, Spring Boot, PostgreSQL, React e TypeScript.",
                "Bacharelado em Ciência da Computação - USP",
                "Idiomas: Inglês Intermediário, Português Nativo"
        );

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "curriculo_rafael.pdf",
                "application/pdf",
                pdfBytes
        );

        // 1. Upload e extração do PDF
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(pdfFile)
                        .header("X-User-Id", userA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXTRACTED"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andReturn();

        JsonNode uploadedJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String resumeId = uploadedJson.get("id").asText();

        // 2. Solicitar análise por IA
        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/analyze")
                        .header("X-User-Id", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.profile.fullName").value("Rafael Almeida"))
                .andExpect(jsonPath("$.profile.confirmed").value(false))
                .andExpect(jsonPath("$.profile.evidences").isArray());

        // 3. Revisar e atualizar manualmente o perfil
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto(
                "Rafael Almeida",
                "Desenvolvedor Full Stack Júnior",
                "Júnior",
                "Desenvolvedor focado em Java, Spring Boot, React e TypeScript.",
                new ContactInfoDto("rafael.almeida@dev.com", "(11) 98765-4321", "São Paulo, SP",
                        "linkedin.com/in/rafaelalmeida", "github.com/rafaelalmeida", ""),
                List.of(),
                List.of(),
                List.of(new SkillItemDto("Java", "LANGUAGE_TECH", "Intermediário", "Java, Spring Boot")),
                List.of(),
                List.of(),
                List.of(),
                List.of("Desenvolvedor Full Stack Júnior", "Desenvolvedor Back-end Júnior")
        );

        mockMvc.perform(put("/api/v1/profile")
                        .header("X-User-Id", userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Desenvolvedor focado em Java, Spring Boot, React e TypeScript."))
                .andExpect(jsonPath("$.confirmed").value(false));

        // 4. Confirmar explicitamente o perfil
        mockMvc.perform(post("/api/v1/profile/confirm")
                        .header("X-User-Id", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());

        // 5. Verificar que o currículo também passou para o estado CONFIRMED
        mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                        .header("X-User-Id", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldIsolateDataBetweenDifferentUsers() throws Exception {
        String user1 = UUID.randomUUID().toString();
        String user2 = UUID.randomUUID().toString();

        byte[] docxBytes = ResumeTextExtractorTest.createDocxWithText(
                "Beatriz Souza",
                "Engenheira de Software Pleno",
                "Experiencia sólida com Java 21, Spring Boot, PostgreSQL e Docker."
        );

        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "beatriz.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        MvcResult res1 = mockMvc.perform(multipart("/api/v1/resumes")
                        .file(docxFile)
                        .header("X-User-Id", user1))
                .andExpect(status().isCreated())
                .andReturn();

        String resumeIdUser1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asText();

        // Usuário 2 não deve conseguir acessar o currículo do Usuário 1
        mockMvc.perform(get("/api/v1/resumes/" + resumeIdUser1)
                        .header("X-User-Id", user2))
                .andExpect(status().isNotFound());

        // Listagem do Usuário 2 deve vir vazia
        MvcResult listUser2 = mockMvc.perform(get("/api/v1/resumes")
                        .header("X-User-Id", user2))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(listUser2.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr).isEmpty();
    }
}
