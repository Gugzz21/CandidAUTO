package com.jobassistant.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.security.allow-test-user-header=false",
        "app.security.local-personal-mode=false"
})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = PostgresUnixSocketBridge.class)
class SecurityAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectUnauthenticatedAccessAndBlockUserIdHeaderSpoofing() throws Exception {
        // 1. Requisição sem token para rota protegida deve retornar 401 Unauthorized
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());

        // 2. Tentativa de falsificar identidade enviando X-User-Id arbitrário sem autenticação deve retornar 403 Forbidden
        mockMvc.perform(get("/api/v1/jobs")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());

        // 3. Token JWT adulterado ou inválido deve retornar 401 Unauthorized
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.assinatura_invalida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAuthenticateWithSignedJwtIsolateUsersAndBlockCrossUserHeaderInjection() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String emailA = "alice." + suffix + "@candidauto.dev";
        String emailB = "bruno." + suffix + "@candidauto.dev";

        // 1. Registrar Usuário A e Usuário B com senha hash BCrypt
        MvcResult regA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Alice Silva",
                                "email", emailA,
                                "password", "SenhaForte@2026"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode jsonA = objectMapper.readTree(regA.getResponse().getContentAsString());
        String userAId = jsonA.get("userId").asText();

        MvcResult regB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bruno Costa",
                                "email", emailB,
                                "password", "OutraSenha@2026"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonB = objectMapper.readTree(regB.getResponse().getContentAsString());
        String tokenB = jsonB.get("token").asText();

        // 2. Login com senha incorreta deve retornar 401; login correto emite JWT válido
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", emailA,
                                "password", "SenhaErrada123"
                        ))))
                .andExpect(status().isUnauthorized());

        MvcResult loginA = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", emailA,
                                "password", "SenhaForte@2026"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String tokenA = objectMapper.readTree(loginA.getResponse().getContentAsString()).get("token").asText();

        // Verificar endpoint /api/v1/auth/me autenticado
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userAId))
                .andExpect(jsonPath("$.email").value(emailA));

        // 3. Alice importa uma vaga usando seu JWT assinado
        ImportJobRequestDto aliceJob = new ImportJobRequestDto(
                JobPlatform.GREENHOUSE,
                "Stripe",
                "Backend Engineer Java",
                "Remoto",
                WorkModality.REMOTO,
                "Júnior",
                "",
                "https://boards.greenhouse.io/stripe/jobs/101",
                "Desenvolvimento com Java 21, Spring Boot e PostgreSQL."
        );

        mockMvc.perform(post("/api/v1/jobs/import")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceJob)))
                .andExpect(status().isCreated());

        // 4. Bruno consulta suas próprias vagas com seu token e vê lista vazia (isolamento estrito)
        MvcResult brunoJobs = mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(brunoJobs.getResponse().getContentAsString())).isEmpty();

        // 5. Se Bruno tentar injetar X-User-Id de Alice junto com seu próprio JWT, o filtro bloqueia com 403 Forbidden
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-User-Id", userAId))
                .andExpect(status().isForbidden());
    }
}
