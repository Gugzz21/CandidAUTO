package com.jobassistant.applications.service;

import com.jobassistant.applications.domain.ApplicationEntity;
import com.jobassistant.applications.domain.ApplicationRepository;
import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.applications.domain.CandidateQaVaultEntity;
import com.jobassistant.applications.domain.CandidateQaVaultRepository;
import com.jobassistant.applications.domain.SubmissionMode;
import com.jobassistant.applications.dto.AnswerMissingQuestionsRequestDto;
import com.jobassistant.applications.dto.ApplicationDto;
import com.jobassistant.applications.dto.CancelApplicationRequestDto;
import com.jobassistant.applications.dto.MissingQuestionDto;
import com.jobassistant.applications.dto.PreparedFieldDto;
import com.jobassistant.applications.dto.QaVaultItemDto;
import com.jobassistant.applications.dto.SubmitApplicationRequestDto;
import com.jobassistant.applications.dto.UpdateApplicationStatusRequestDto;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.dto.ApplicationQuestionDto;
import com.jobassistant.jobs.service.JobDiscoveryService;
import com.jobassistant.matching.domain.JobMatchAnalysisEntity;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.matching.dto.JobMatchAnalysisDto;
import com.jobassistant.matching.service.JobMatchingEngineService;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.security.CurrentUserProvider;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SmartApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateQaVaultRepository qaVaultRepository;
    private final JobPostingRepository jobRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobMatchAnalysisRepository matchRepository;
    private final JobMatchingEngineService matchingService;
    private final JobDiscoveryService discoveryService;
    private final CurrentUserProvider currentUserProvider;

    public SmartApplicationService(
            ApplicationRepository applicationRepository,
            CandidateQaVaultRepository qaVaultRepository,
            JobPostingRepository jobRepository,
            CandidateProfileRepository profileRepository,
            JobMatchAnalysisRepository matchRepository,
            JobMatchingEngineService matchingService,
            JobDiscoveryService discoveryService,
            CurrentUserProvider currentUserProvider
    ) {
        this.applicationRepository = applicationRepository;
        this.qaVaultRepository = qaVaultRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.matchRepository = matchRepository;
        this.matchingService = matchingService;
        this.discoveryService = discoveryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ApplicationDto prepareApplicationForJob(UUID jobId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        JobPostingEntity job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para o usuário atual."));
        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cadastre e revise seu perfil profissional antes de preparar uma candidatura."));
        if (!profile.isConfirmed()) {
            throw new BadRequestException("Regra de segurança: confirme explicitamente seu perfil profissional na aba 'Validação' antes de preparar uma candidatura.");
        }

        if (!job.isActive()) {
            throw new BadRequestException("Esta oportunidade encontra-se encerrada ou indisponível.");
        }

        // Garantir que a análise de compatibilidade exista
        JobMatchAnalysisEntity matchEntity = matchRepository.findByUserIdAndJobId(userId, jobId)
                .orElseGet(() -> matchingService.computeAndSaveMatch(userId, profile, job));

        Map<String, String> vaultMap = loadUserVaultMap(userId);

        List<PreparedFieldDto> preparedFields = new ArrayList<>();
        List<MissingQuestionDto> missingQuestions = new ArrayList<>();

        ContactInfoDto contact = profile.getContactInfo() != null ? profile.getContactInfo() : ContactInfoDto.empty();

        // 1. Preencher campos padrão de candidatura com dados reais do perfil
        addStandardField(preparedFields, missingQuestions, "first_name_last_name", "Nome Completo",
                profile.getFullName(), "Extraído e confirmado no Perfil Profissional", true);
        addStandardField(preparedFields, missingQuestions, "email", "E-mail",
                contact.email(), "Extraído e confirmado no Perfil Profissional", true);
        addStandardField(preparedFields, missingQuestions, "phone", "Telefone / WhatsApp",
                contact.phone(), "Extraído e confirmado no Perfil Profissional", true);
        addStandardField(preparedFields, missingQuestions, "location", "Cidade / Estado",
                contact.location(), "Extraído e confirmado no Perfil Profissional", false);
        addStandardField(preparedFields, missingQuestions, "linkedin_url", "Perfil do LinkedIn",
                contact.linkedinUrl(), "Extraído e confirmado no Perfil Profissional", false);
        addStandardField(preparedFields, missingQuestions, "github_url", "Perfil do GitHub / Portfólio",
                contact.githubUrl(), "Extraído e confirmado no Perfil Profissional", false);

        String pitch = buildTruthfulCoverPitch(profile, job);
        preparedFields.add(new PreparedFieldDto(
                "cover_summary",
                "Resumo de Apresentação (Baseado exclusivamente no currículo)",
                pitch,
                "Gerado estritamente com as competências confirmadas do perfil",
                false,
                false
        ));

        // 2. Avaliar perguntas específicas da vaga (Greenhouse / Lever / Gupy / Custom)
        List<ApplicationQuestionDto> jobQuestions = job.getApplicationQuestions() != null
                ? job.getApplicationQuestions()
                : List.of();

        // Caso a vaga não traga perguntas explícitas da API, verificamos perguntas de recrutamento recorrentes na descrição
        List<ApplicationQuestionDto> effectiveQuestions = new ArrayList<>(jobQuestions);
        if (effectiveQuestions.isEmpty()) {
            effectiveQuestions.add(new ApplicationQuestionDto(
                    "pretensao_salarial",
                    "Qual sua pretensão salarial para esta posição?",
                    "text",
                    true,
                    List.of()
            ));
            effectiveQuestions.add(new ApplicationQuestionDto(
                    "disponibilidade_inicio",
                    "Qual sua disponibilidade para início / aviso prévio?",
                    "text",
                    true,
                    List.of()
            ));
        }

        for (ApplicationQuestionDto q : effectiveQuestions) {
            String key = normalizeQuestionKey(q.name() != null && !q.name().isBlank() ? q.name() : q.label());
            // Evitar duplicar campos básicos já preenchidos
            if (isStandardFieldKey(key)) {
                continue;
            }

            String matchedAnswer = resolveTruthfulAnswerForQuestion(q.label(), profile, vaultMap);
            if (matchedAnswer != null && !matchedAnswer.isBlank()) {
                preparedFields.add(new PreparedFieldDto(
                        key,
                        q.label(),
                        matchedAnswer,
                        "Respondido com dado confirmado do Perfil / Cofre do Candidato",
                        q.required(),
                        false
                ));
            } else {
                preparedFields.add(new PreparedFieldDto(
                        key,
                        q.label(),
                        "",
                        "Aguardando resposta verdadeira do candidato (a IA não inventa informações)",
                        q.required(),
                        true
                ));
                missingQuestions.add(new MissingQuestionDto(
                        key,
                        q.label(),
                        q.required(),
                        "Esta informação não consta no currículo nem no cofre de respostas confirmadas."
                ));
            }
        }

        SubmissionMode mode = resolveSubmissionMode(job.getSourcePlatform());
        ApplicationStatus targetStatus = missingQuestions.isEmpty()
                ? ApplicationStatus.PRONTA_PARA_CANDIDATURA
                : ApplicationStatus.AGUARDANDO_REVISAO;

        ApplicationEntity app = applicationRepository.findByUserIdAndJobId(userId, jobId)
                .orElseGet(() -> new ApplicationEntity(UUID.randomUUID(), userId, jobId, targetStatus, mode));

        app.setSubmissionMode(mode);
        app.setPreparedFields(preparedFields);
        app.setMissingQuestions(missingQuestions);
        app.setStatus(targetStatus);
        app.appendAuditEvent(
                targetStatus,
                "PREPARACAO_IA",
                missingQuestions.isEmpty()
                        ? "Todos os campos foram preenchidos com dados confirmados. Candidatura pronta para revisão final."
                        : "Foram identificadas " + missingQuestions.size() + " pergunta(s) que exigem confirmação manual do candidato."
        );

        ApplicationEntity saved = applicationRepository.save(app);
        return toDto(saved, job, matchingService.toDto(matchEntity));
    }

    @Transactional
    public ApplicationDto answerMissingQuestions(UUID applicationId, AnswerMissingQuestionsRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ApplicationEntity app = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada."));
        JobPostingEntity job = jobRepository.findByIdAndUserId(app.getJobId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga vinculada não encontrada."));

        Map<String, String> provided = request.answers() != null ? request.answers() : Map.of();

        List<PreparedFieldDto> updatedFields = new ArrayList<>();
        for (PreparedFieldDto field : app.getPreparedFields()) {
            String newVal = provided.get(field.fieldName());
            if (newVal != null && !newVal.isBlank()) {
                updatedFields.add(new PreparedFieldDto(
                        field.fieldName(),
                        field.fieldLabel(),
                        newVal.trim(),
                        "Confirmado diretamente pelo candidato",
                        field.required(),
                        false
                ));
                if (request.saveToVault()) {
                    saveOrUpdateVaultAnswer(userId, field.fieldName(), field.fieldLabel(), newVal.trim());
                }
            } else {
                updatedFields.add(field);
            }
        }

        List<MissingQuestionDto> remainingMissing = app.getMissingQuestions()
                .stream()
                .filter(mq -> {
                    String val = provided.get(mq.questionKey());
                    return val == null || val.isBlank();
                })
                .toList();

        app.setPreparedFields(updatedFields);
        app.setMissingQuestions(remainingMissing);

        ApplicationStatus nextStatus = remainingMissing.isEmpty()
                ? ApplicationStatus.PRONTA_PARA_CANDIDATURA
                : ApplicationStatus.AGUARDANDO_REVISAO;
        app.setStatus(nextStatus);
        app.appendAuditEvent(
                nextStatus,
                "RESPOSTAS_CANDIDATO",
                remainingMissing.isEmpty()
                        ? "Todas as perguntas pendentes foram respondidas pelo candidato. Pronta para revisão final."
                        : "Respostas parciais salvas; restam " + remainingMissing.size() + " pergunta(s)."
        );

        ApplicationEntity saved = applicationRepository.save(app);
        JobMatchAnalysisDto matchDto = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .map(matchingService::toDto)
                .orElse(null);
        return toDto(saved, job, matchDto);
    }

    @Transactional
    public ApplicationDto submitApplication(UUID applicationId, SubmitApplicationRequestDto request) {
        if (request == null || !request.explicitApprovalConfirmed()) {
            throw new BadRequestException(
                    "Regra obrigatória de segurança: nenhuma candidatura pode ser enviada sem a aprovação explícita do usuário."
            );
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        ApplicationEntity app = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada."));
        JobPostingEntity job = jobRepository.findByIdAndUserId(app.getJobId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga vinculada não encontrada."));

        boolean hasUnansweredRequired = app.getMissingQuestions()
                .stream()
                .anyMatch(MissingQuestionDto::required);
        if (hasUnansweredRequired) {
            throw new BadRequestException(
                    "Existem perguntas obrigatórias sem resposta confirmada. Preencha-as antes de confirmar o envio."
            );
        }

        Instant now = Instant.now();
        app.setUserApprovedExplicitly(true);
        app.setStatus(ApplicationStatus.CANDIDATURA_ENVIADA);
        app.setSubmittedAt(now);
        if (request.notes() != null && !request.notes().isBlank()) {
            app.setNotes(request.notes().trim());
        }

        String receipt = "Candidatura aprovada explicitamente e registrada em " + now
                + " | Empresa: " + job.getCompanyName()
                + " | Cargo: " + job.getTitle()
                + " | Modo: " + app.getSubmissionMode()
                + " | Link Original: " + job.getOriginalUrl();
        app.setSubmissionReceipt(receipt);
        app.appendAuditEvent(
                ApplicationStatus.CANDIDATURA_ENVIADA,
                "BOTAO_CANDIDATAR_SE_APROVADO",
                receipt
        );

        ApplicationEntity saved = applicationRepository.save(app);
        JobMatchAnalysisDto matchDto = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .map(matchingService::toDto)
                .orElse(null);
        return toDto(saved, job, matchDto);
    }

    @Transactional
    public ApplicationDto cancelApplication(UUID applicationId, CancelApplicationRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ApplicationEntity app = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada."));
        JobPostingEntity job = jobRepository.findByIdAndUserId(app.getJobId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga vinculada não encontrada."));

        boolean keepDraft = request != null && request.keepAsDraft();
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Interrompido pelo usuário na tela de revisão.";

        if (keepDraft) {
            app.setStatus(ApplicationStatus.AGUARDANDO_REVISAO);
            app.appendAuditEvent(
                    ApplicationStatus.AGUARDANDO_REVISAO,
                    "BOTAO_CANCELAR_RASCUNHO",
                    "Envio interrompido (nenhum dado enviado à empresa). Mantido como rascunho. Motivo: " + reason
            );
        } else {
            app.setStatus(ApplicationStatus.CANCELADA);
            app.appendAuditEvent(
                    ApplicationStatus.CANCELADA,
                    "BOTAO_CANCELAR_DESCARTADO",
                    "Candidatura cancelada pelo usuário (nenhum dado enviado à empresa). Motivo: " + reason
            );
        }

        ApplicationEntity saved = applicationRepository.save(app);
        JobMatchAnalysisDto matchDto = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .map(matchingService::toDto)
                .orElse(null);
        return toDto(saved, job, matchDto);
    }

    @Transactional
    public ApplicationDto updateApplicationStatus(UUID applicationId, UpdateApplicationStatusRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ApplicationEntity app = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidatura não encontrada."));
        JobPostingEntity job = jobRepository.findByIdAndUserId(app.getJobId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga vinculada não encontrada."));

        if (request.status() == ApplicationStatus.CANDIDATURA_ENVIADA && !app.isUserApprovedExplicitly()) {
            throw new BadRequestException("Para enviar uma candidatura, utilize o fluxo de aprovação explícita (Candidatar-se).");
        }

        app.setStatus(request.status());
        if (request.notes() != null && !request.notes().isBlank()) {
            app.setNotes(request.notes().trim());
        }
        if (request.status() == ApplicationStatus.VAGA_ENCERRADA) {
            job.setActive(false);
            jobRepository.save(job);
        }

        app.appendAuditEvent(
                request.status(),
                "ATUALIZACAO_ACOMPANHAMENTO",
                request.notes() != null && !request.notes().isBlank()
                        ? request.notes().trim()
                        : "Status atualizado para " + request.status().name()
        );

        ApplicationEntity saved = applicationRepository.save(app);
        JobMatchAnalysisDto matchDto = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .map(matchingService::toDto)
                .orElse(null);
        return toDto(saved, job, matchDto);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> listUserApplications(ApplicationStatus statusFilter) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return applicationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .filter(app -> statusFilter == null || app.getStatus() == statusFilter)
                .map(app -> {
                    JobPostingEntity job = jobRepository.findById(app.getJobId()).orElse(null);
                    if (job == null) return null;
                    JobMatchAnalysisDto matchDto = matchRepository.findByUserIdAndJobId(userId, job.getId())
                            .map(matchingService::toDto)
                            .orElse(null);
                    return toDto(app, job, matchDto);
                })
                .filter(dto -> dto != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QaVaultItemDto> listUserQaVault() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return qaVaultRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(v -> new QaVaultItemDto(
                        v.getId(),
                        v.getQuestionKey(),
                        v.getQuestionLabel(),
                        v.getConfirmedAnswer(),
                        v.getUpdatedAt()
                ))
                .toList();
    }

    private void addStandardField(
            List<PreparedFieldDto> prepared,
            List<MissingQuestionDto> missing,
            String fieldName,
            String fieldLabel,
            String rawVal,
            String source,
            boolean required
    ) {
        String val = rawVal != null ? rawVal.trim() : "";
        if (!val.isEmpty()) {
            prepared.add(new PreparedFieldDto(fieldName, fieldLabel, val, source, required, false));
        } else if (required) {
            prepared.add(new PreparedFieldDto(fieldName, fieldLabel, "", "Informação obrigatória ausente no perfil", true, true));
            missing.add(new MissingQuestionDto(fieldName, fieldLabel, true, "Dado obrigatório não preenchido no Perfil Profissional."));
        }
    }

    private String resolveTruthfulAnswerForQuestion(
            String label,
            CandidateProfileEntity profile,
            Map<String, String> vaultMap
    ) {
        String normKey = normalizeQuestionKey(label);
        if (vaultMap.containsKey(normKey)) {
            return vaultMap.get(normKey);
        }
        for (Map.Entry<String, String> entry : vaultMap.entrySet()) {
            if (normKey.contains(entry.getKey()) || entry.getKey().contains(normKey)) {
                return entry.getValue();
            }
        }

        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.contains("inglês") || lower.contains("english")) {
            if (profile.getLanguages() != null) {
                for (LanguageItemDto lang : profile.getLanguages()) {
                    if (lang.language() != null && lang.language().toLowerCase(Locale.ROOT).contains("ingl")) {
                        return lang.language() + " - " + lang.proficiency();
                    }
                }
            }
        }
        return null;
    }

    private String buildTruthfulCoverPitch(CandidateProfileEntity profile, JobPostingEntity job) {
        List<String> candidateSkillNames = profile.getSkills() != null
                ? profile.getSkills().stream().map(SkillItemDto::name).limit(6).toList()
                : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("Candidato(a): ").append(profile.getFullName());
        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            sb.append(" (").append(profile.getHeadline()).append(")");
        }
        sb.append(". ");
        if (profile.getSummary() != null && !profile.getSummary().isBlank()) {
            sb.append(profile.getSummary()).append(" ");
        }
        if (!candidateSkillNames.isEmpty()) {
            sb.append("Competências comprovadas no currículo: ").append(String.join(", ", candidateSkillNames)).append(".");
        }
        return sb.toString().trim();
    }

    private SubmissionMode resolveSubmissionMode(JobPlatform platform) {
        if (platform == JobPlatform.GREENHOUSE || platform == JobPlatform.LEVER) {
            return SubmissionMode.INTEGRACAO_AUTORIZADA;
        }
        return SubmissionMode.FLUXO_ASSISTIDO;
    }

    private boolean isStandardFieldKey(String key) {
        return key.equals("name") || key.equals("nome_completo") || key.equals("email")
                || key.equals("e_mail") || key.equals("phone") || key.equals("telefone")
                || key.equals("urls_linkedin") || key.equals("urls_github") || key.equals("comments");
    }

    private String normalizeQuestionKey(String raw) {
        if (raw == null) return "question";
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private Map<String, String> loadUserVaultMap(UUID userId) {
        Map<String, String> map = new LinkedHashMap<>();
        for (CandidateQaVaultEntity item : qaVaultRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)) {
            map.put(item.getQuestionKey(), item.getConfirmedAnswer());
        }
        return map;
    }

    private void saveOrUpdateVaultAnswer(UUID userId, String key, String label, String answer) {
        String normKey = normalizeQuestionKey(key);
        Optional<CandidateQaVaultEntity> existing = qaVaultRepository.findByUserIdAndQuestionKey(userId, normKey);
        if (existing.isPresent()) {
            CandidateQaVaultEntity entity = existing.get();
            entity.setQuestionLabel(label);
            entity.setConfirmedAnswer(answer);
            entity.setUpdatedAt(Instant.now());
            qaVaultRepository.save(entity);
        } else {
            qaVaultRepository.save(new CandidateQaVaultEntity(
                    UUID.randomUUID(),
                    userId,
                    normKey,
                    label,
                    answer
            ));
        }
    }

    public ApplicationDto toDto(ApplicationEntity entity, JobPostingEntity job, JobMatchAnalysisDto matchDto) {
        return new ApplicationDto(
                entity.getId(),
                discoveryService.toJobDto(job),
                matchDto,
                entity.getStatus(),
                entity.getSubmissionMode(),
                entity.getPreparedFields(),
                entity.getMissingQuestions(),
                entity.isUserApprovedExplicitly(),
                entity.getSubmittedAt(),
                entity.getSubmissionReceipt(),
                entity.getNotes(),
                entity.getAuditLog(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
