package com.jobassistant.ai.service;

import com.jobassistant.ai.client.LlmClient;
import com.jobassistant.ai.dto.AiStructuredProfileResponse;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.domain.ProfileEvidenceEntity;
import com.jobassistant.profile.dto.CertificationItemDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.ExperienceItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.ProjectItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.profile.repository.ProfileEvidenceRepository;
import com.jobassistant.shared.FileProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiProfileAnalysisService {

    private final LlmClient llmClient;
    private final CandidateProfileRepository profileRepository;
    private final ProfileEvidenceRepository evidenceRepository;

    public AiProfileAnalysisService(
            LlmClient llmClient,
            CandidateProfileRepository profileRepository,
            ProfileEvidenceRepository evidenceRepository
    ) {
        this.llmClient = llmClient;
        this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public CandidateProfileEntity analyzeAndPersistProfile(UUID userId, UUID resumeId, String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new FileProcessingException("O currículo não possui texto extraído para análise pela IA.");
        }

        AiStructuredProfileResponse aiResult = llmClient.extractProfileFromResume(resumeText);
        if (aiResult == null) {
            throw new FileProcessingException("A resposta da análise de IA veio nula ou inválida.");
        }

        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new CandidateProfileEntity(UUID.randomUUID(), userId));

        profile.setResumeId(resumeId);
        profile.setFullName(safeString(aiResult.fullName()));
        profile.setHeadline(safeString(aiResult.headline()));
        profile.setSeniorityLevel(safeString(aiResult.seniorityLevel()));
        profile.setSummary(safeString(aiResult.summary()));
        profile.setContactInfo(aiResult.contactInfo() != null ? aiResult.contactInfo() : ContactInfoDto.empty());
        profile.setExperiences(aiResult.experiences() != null ? aiResult.experiences() : List.of());
        profile.setEducation(aiResult.education() != null ? aiResult.education() : List.of());
        profile.setSkills(aiResult.skills() != null ? aiResult.skills() : List.of());
        profile.setLanguages(aiResult.languages() != null ? aiResult.languages() : List.of());
        profile.setCertifications(aiResult.certifications() != null ? aiResult.certifications() : List.of());
        profile.setProjects(aiResult.projects() != null ? aiResult.projects() : List.of());
        profile.setTargetRoles(aiResult.targetRoles() != null ? aiResult.targetRoles() : List.of());
        profile.setConfirmed(false);
        profile.setConfirmedAt(null);
        profile.setUpdatedAt(Instant.now());

        CandidateProfileEntity savedProfile = profileRepository.save(profile);

        evidenceRepository.deleteAllByProfileId(savedProfile.getId());
        List<ProfileEvidenceEntity> evidences = buildVerifiedEvidences(savedProfile.getId(), aiResult, resumeText);
        evidenceRepository.saveAll(evidences);

        return savedProfile;
    }

    public List<ProfileEvidenceEntity> buildVerifiedEvidences(
            UUID profileId,
            AiStructuredProfileResponse aiResult,
            String sourceText
    ) {
        List<ProfileEvidenceEntity> list = new ArrayList<>();
        String normalizedSource = normalizeForMatch(sourceText);

        addScalarEvidence(list, profileId, "fullName", "Nome Completo",
                aiResult.fullName(), aiResult.fullNameEvidence(), normalizedSource, true);
        addScalarEvidence(list, profileId, "headline", "Título Profissional / Cargo Alvo",
                aiResult.headline(), aiResult.headlineEvidence(), normalizedSource, true);
        addScalarEvidence(list, profileId, "seniorityLevel", "Senioridade Profissional",
                aiResult.seniorityLevel(), aiResult.seniorityEvidence(), normalizedSource, true);
        addScalarEvidence(list, profileId, "summary", "Resumo Profissional",
                aiResult.summary(), aiResult.summaryEvidence(), normalizedSource, false);

        ContactInfoDto contact = aiResult.contactInfo() != null ? aiResult.contactInfo() : ContactInfoDto.empty();
        addScalarEvidence(list, profileId, "contactInfo.email", "E-mail de Contato",
                contact.email(), contact.email(), normalizedSource, true);
        addScalarEvidence(list, profileId, "contactInfo.phone", "Telefone",
                contact.phone(), contact.phone(), normalizedSource, false);
        addScalarEvidence(list, profileId, "contactInfo.location", "Localização",
                contact.location(), contact.location(), normalizedSource, true);

        List<ExperienceItemDto> experiences = aiResult.experiences() != null ? aiResult.experiences() : List.of();
        if (experiences.isEmpty()) {
            list.add(createReviewRequiredEvidence(profileId, "experiences", "Experiências Profissionais",
                    "", "Nenhuma experiência estruturada pôde ser confirmada automaticamente."));
        } else {
            for (int i = 0; i < experiences.size(); i++) {
                ExperienceItemDto exp = experiences.get(i);
                String val = exp.role() + " @ " + exp.company() + " (" + exp.startDate() + " - " + exp.endDate() + ")";
                addScalarEvidence(list, profileId, "experiences[" + i + "]", "Experiência #" + (i + 1),
                        val, exp.evidenceExcerpt(), normalizedSource, true);
            }
        }

        List<EducationItemDto> education = aiResult.education() != null ? aiResult.education() : List.of();
        if (education.isEmpty()) {
            list.add(createReviewRequiredEvidence(profileId, "education", "Formação Acadêmica",
                    "", "Nenhuma formação acadêmica pôde ser confirmada automaticamente."));
        } else {
            for (int i = 0; i < education.size(); i++) {
                EducationItemDto edu = education.get(i);
                addScalarEvidence(list, profileId, "education[" + i + "]", "Formação #" + (i + 1),
                        edu.degree() + " - " + edu.institution(), edu.evidenceExcerpt(), normalizedSource, true);
            }
        }

        List<SkillItemDto> skills = aiResult.skills() != null ? aiResult.skills() : List.of();
        if (skills.isEmpty()) {
            list.add(createReviewRequiredEvidence(profileId, "skills", "Tecnologias e Competências",
                    "", "Nenhuma competência técnica foi identificada automaticamente."));
        } else {
            for (int i = 0; i < skills.size(); i++) {
                SkillItemDto skill = skills.get(i);
                addScalarEvidence(list, profileId, "skills[" + i + "]", "Competência: " + skill.name(),
                        skill.name(), skill.evidenceExcerpt(), normalizedSource, false);
            }
        }

        List<LanguageItemDto> languages = aiResult.languages() != null ? aiResult.languages() : List.of();
        for (int i = 0; i < languages.size(); i++) {
            LanguageItemDto lang = languages.get(i);
            addScalarEvidence(list, profileId, "languages[" + i + "]", "Idioma: " + lang.language(),
                    lang.language() + " (" + lang.proficiency() + ")", lang.evidenceExcerpt(), normalizedSource, false);
        }

        List<CertificationItemDto> certifications = aiResult.certifications() != null ? aiResult.certifications() : List.of();
        for (int i = 0; i < certifications.size(); i++) {
            CertificationItemDto cert = certifications.get(i);
            addScalarEvidence(list, profileId, "certifications[" + i + "]", "Certificação #" + (i + 1),
                    cert.name(), cert.evidenceExcerpt(), normalizedSource, false);
        }

        List<ProjectItemDto> projects = aiResult.projects() != null ? aiResult.projects() : List.of();
        for (int i = 0; i < projects.size(); i++) {
            ProjectItemDto proj = projects.get(i);
            addScalarEvidence(list, profileId, "projects[" + i + "]", "Projeto #" + (i + 1),
                    proj.name(), proj.evidenceExcerpt(), normalizedSource, false);
        }

        return list;
    }

    private void addScalarEvidence(
            List<ProfileEvidenceEntity> list,
            UUID profileId,
            String fieldPath,
            String fieldLabel,
            String extractedValue,
            String evidenceText,
            String normalizedSource,
            boolean mandatoryForReview
    ) {
        String cleanVal = safeString(extractedValue);
        String cleanEv = safeString(evidenceText);

        if (cleanVal.isEmpty()) {
            if (mandatoryForReview) {
                list.add(createReviewRequiredEvidence(profileId, fieldPath, fieldLabel, "",
                        "Campo ausente no currículo ou não identificado com segurança. Preencha ou revise manualmente."));
            }
            return;
        }

        boolean verified = !cleanEv.isEmpty() && isEvidencePresentInSource(cleanEv, normalizedSource);
        boolean needsReview = !verified;

        list.add(new ProfileEvidenceEntity(
                UUID.randomUUID(),
                profileId,
                fieldPath,
                fieldLabel,
                cleanVal,
                verified ? cleanEv : (cleanEv.isEmpty() ? "Sem evidência textual direta no documento." : cleanEv + " [Evidência não encontrada literalmente no documento]"),
                verified,
                needsReview,
                Instant.now()
        ));
    }

    private ProfileEvidenceEntity createReviewRequiredEvidence(
            UUID profileId,
            String fieldPath,
            String fieldLabel,
            String extractedValue,
            String reason
    ) {
        return new ProfileEvidenceEntity(
                UUID.randomUUID(),
                profileId,
                fieldPath,
                fieldLabel,
                extractedValue,
                reason,
                false,
                true,
                Instant.now()
        );
    }

    public boolean isEvidencePresentInSource(String evidenceText, String normalizedSource) {
        String normEv = normalizeForMatch(evidenceText);
        if (normEv.length() < 2) {
            return false;
        }
        return normalizedSource.contains(normEv);
    }

    private String normalizeForMatch(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeString(String s) {
        return s != null ? s.trim() : "";
    }
}
