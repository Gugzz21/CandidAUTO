package com.jobassistant.ai.service;

import com.jobassistant.ai.client.LlmClient;
import com.jobassistant.ai.dto.AiStructuredProfileResponse;
import com.jobassistant.profile.domain.ProfileEvidenceEntity;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.profile.repository.ProfileEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiProfileAnalysisServiceTest {

    private AiProfileAnalysisService service;

    @BeforeEach
    void setUp() {
        LlmClient llmClient = Mockito.mock(LlmClient.class);
        CandidateProfileRepository profileRepository = Mockito.mock(CandidateProfileRepository.class);
        ProfileEvidenceRepository evidenceRepository = Mockito.mock(ProfileEvidenceRepository.class);
        service = new AiProfileAnalysisService(llmClient, profileRepository, evidenceRepository);
    }

    @Test
    void shouldVerifyEvidencesAgainstSourceResumeTextAndFlagUnverifiedFields() {
        String sourceResume = """
                Lucas Oliveira
                Desenvolvedor Back-end Júnior
                lucas@email.com | São Paulo, SP
                Tecnologias: Java, Spring Boot, PostgreSQL
                """;

        AiStructuredProfileResponse response = new AiStructuredProfileResponse(
                "Lucas Oliveira",
                "Lucas Oliveira",
                "Desenvolvedor Back-end Júnior",
                "Desenvolvedor Back-end Júnior",
                "Júnior",
                "Desenvolvedor Back-end Júnior",
                "",
                "",
                new ContactInfoDto("lucas@email.com", "", "São Paulo, SP", "", "", ""),
                List.of(),
                List.of(),
                List.of(
                        new SkillItemDto("Java", "LANGUAGE_TECH", "Intermediário", "Tecnologias: Java, Spring Boot, PostgreSQL"),
                        new SkillItemDto("Kubernetes", "TOOL", "Avançado", "Experiência inventada com Kubernetes em produção")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of("Desenvolvedor Back-end Júnior")
        );

        List<ProfileEvidenceEntity> evidences = service.buildVerifiedEvidences(
                UUID.randomUUID(),
                response,
                sourceResume
        );

        ProfileEvidenceEntity javaSkillEv = evidences.stream()
                .filter(e -> "skills[0]".equals(e.getFieldPath()))
                .findFirst()
                .orElseThrow();
        assertThat(javaSkillEv.isVerified()).isTrue();
        assertThat(javaSkillEv.isNeedsReview()).isFalse();

        // A skill "Kubernetes" cuja evidência NÃO existe no texto original deve ser marcada como não verificada!
        ProfileEvidenceEntity k8sSkillEv = evidences.stream()
                .filter(e -> "skills[1]".equals(e.getFieldPath()))
                .findFirst()
                .orElseThrow();
        assertThat(k8sSkillEv.isVerified()).isFalse();
        assertThat(k8sSkillEv.isNeedsReview()).isTrue();
    }
}
