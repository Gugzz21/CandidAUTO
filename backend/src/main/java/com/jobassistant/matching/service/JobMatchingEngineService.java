package com.jobassistant.matching.service;

import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.matching.domain.JobMatchAnalysisEntity;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.matching.dto.JobMatchAnalysisDto;
import com.jobassistant.matching.dto.ScoreBreakdownDto;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.ExperienceItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.ProjectItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.security.CurrentUserProvider;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class JobMatchingEngineService {

    private final JobPostingRepository jobRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobMatchAnalysisRepository matchRepository;
    private final CurrentUserProvider currentUserProvider;

    public JobMatchingEngineService(
            JobPostingRepository jobRepository,
            CandidateProfileRepository profileRepository,
            JobMatchAnalysisRepository matchRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.matchRepository = matchRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public JobMatchAnalysisDto analyzeSingleJob(UUID jobId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        JobPostingEntity job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para o usuário atual."));
        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cadastre seu perfil profissional antes de executar a análise de compatibilidade."));
        if (!profile.isConfirmed()) {
            throw new BadRequestException("Confirme explicitamente seu perfil profissional na aba 'Validação' antes de analisar a compatibilidade com vagas.");
        }

        JobMatchAnalysisEntity saved = computeAndSaveMatch(userId, profile, job);
        return toDto(saved);
    }

    @Transactional
    public List<JobMatchAnalysisDto> analyzeAllActiveJobs() {
        UUID userId = currentUserProvider.getCurrentUserId();
        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cadastre seu perfil profissional antes de executar a análise de compatibilidade."));
        if (!profile.isConfirmed()) {
            throw new BadRequestException("Confirme explicitamente seu perfil profissional na aba 'Validação' antes de analisar a compatibilidade com vagas.");
        }

        List<JobPostingEntity> activeJobs = jobRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId)
                .stream()
                .filter(JobPostingEntity::isActive)
                .toList();

        List<JobMatchAnalysisDto> results = new ArrayList<>();
        for (JobPostingEntity job : activeJobs) {
            JobMatchAnalysisEntity saved = computeAndSaveMatch(userId, profile, job);
            results.add(toDto(saved));
        }

        return results.stream()
                .sorted((a, b) -> Integer.compare(b.compatibilityScore(), a.compatibilityScore()))
                .toList();
    }

    @Transactional
    public JobMatchAnalysisEntity computeAndSaveMatch(
            UUID userId,
            CandidateProfileEntity profile,
            JobPostingEntity job
    ) {
        Set<String> candidateSkillsLower = collectCandidateSkillsLower(profile);
        String candidateCorpusLower = buildCandidateCorpusLower(profile);

        List<String> positivePoints = new ArrayList<>();
        List<String> attentionPoints = new ArrayList<>();
        List<String> matchedRequirements = new ArrayList<>();
        List<String> missingRequirements = new ArrayList<>();

        // 1. Tecnologias e Requisitos Obrigatórios (até 50 pontos)
        List<String> jobTechs = job.getTechnologies();
        List<String> matchedTechs = new ArrayList<>();
        List<String> missingTechs = new ArrayList<>();

        for (String tech : jobTechs) {
            String lower = tech.toLowerCase(Locale.ROOT);
            if (candidateSkillsLower.contains(lower) || candidateCorpusLower.contains(lower)) {
                matchedTechs.add(tech);
                matchedRequirements.add("Tecnologia atendida: " + tech);
            } else {
                missingTechs.add(tech);
            }
        }

        // Verificar requisitos obrigatórios textuais adicionais
        for (String req : job.getMandatoryRequirements()) {
            String reqLower = req.toLowerCase(Locale.ROOT);
            boolean satisfied = matchedTechs.stream().anyMatch(t -> reqLower.contains(t.toLowerCase(Locale.ROOT)))
                    || hasKeywordOverlap(reqLower, candidateCorpusLower);
            if (satisfied) {
                if (!matchedRequirements.contains(req)) {
                    matchedRequirements.add(req);
                }
            } else {
                missingRequirements.add(req);
            }
        }

        int techScore;
        if (jobTechs.isEmpty() && job.getMandatoryRequirements().isEmpty()) {
            techScore = 40;
        } else {
            int totalCriteria = Math.max(1, jobTechs.size());
            techScore = (int) Math.round(50.0 * matchedTechs.size() / totalCriteria);
        }

        if (!matchedTechs.isEmpty()) {
            positivePoints.add("Domínio / experiência comprovada com: " + String.join(", ", matchedTechs) + ".");
        }
        for (String missingTech : missingTechs) {
            boolean isOnlyDesirable = job.getDesirableRequirements().stream()
                    .anyMatch(d -> d.toLowerCase(Locale.ROOT).contains(missingTech.toLowerCase(Locale.ROOT)));
            if (isOnlyDesirable) {
                attentionPoints.add("Conhecimento de " + missingTech + " é listado como desejável (diferencial).");
            } else {
                attentionPoints.add("A empresa solicita experiência ou conhecimento com " + missingTech + ".");
                String msg = "Tecnologia solicitada não encontrada no perfil: " + missingTech;
                if (!missingRequirements.contains(msg)) {
                    missingRequirements.add(msg);
                }
            }
        }

        // 2. Senioridade e Cargo (até 20 pontos)
        int seniorityScore = evaluateSeniority(profile.getSeniorityLevel(), job.getSeniority(), positivePoints, attentionPoints);

        // 3. Modalidade e Localização (até 15 pontos)
        int modalityScore = evaluateModalityAndLocation(profile, job, positivePoints, attentionPoints);

        // 4. Formação Acadêmica e Idiomas (até 15 pontos)
        int eduScore = evaluateEducationAndLanguages(profile, job, positivePoints, attentionPoints);

        // 5. Bônus de Requisitos Desejáveis (até +10 pontos, sem penalizar ausências)
        int desirableBonus = 0;
        for (String des : job.getDesirableRequirements()) {
            String desLower = des.toLowerCase(Locale.ROOT);
            if (hasKeywordOverlap(desLower, candidateCorpusLower)) {
                desirableBonus += 5;
                positivePoints.add("Diferencial atendido: " + des);
            } else if (attentionPoints.stream().noneMatch(a -> a.contains(des))) {
                attentionPoints.add("Diferencial desejável pela vaga: " + des);
            }
        }
        desirableBonus = Math.min(10, desirableBonus);

        int rawTotal = techScore + seniorityScore + modalityScore + eduScore + desirableBonus;
        int finalScore = Math.max(0, Math.min(100, rawTotal));

        String summary = buildExplainableSummary(job, finalScore, matchedTechs, missingTechs, seniorityScore);

        JobMatchAnalysisEntity entity = matchRepository.findByUserIdAndJobId(userId, job.getId())
                .orElseGet(() -> new JobMatchAnalysisEntity(UUID.randomUUID(), userId, job.getId(), profile.getId()));

        entity.setProfileId(profile.getId());
        entity.setCompatibilityScore(finalScore);
        entity.setScoreBreakdown(new ScoreBreakdownDto(techScore, seniorityScore, modalityScore, eduScore, desirableBonus));
        entity.setPositivePoints(positivePoints);
        entity.setAttentionPoints(attentionPoints);
        entity.setMatchedRequirements(matchedRequirements);
        entity.setMissingRequirements(missingRequirements);
        entity.setAiSummary(summary);
        entity.setAnalyzedAt(Instant.now());

        return matchRepository.save(entity);
    }

    private int evaluateSeniority(
            String candidateSeniority,
            String jobSeniority,
            List<String> positivePoints,
            List<String> attentionPoints
    ) {
        int candRank = rankSeniority(candidateSeniority);
        int jobRank = rankSeniority(jobSeniority);

        if (candRank == jobRank) {
            positivePoints.add("Senioridade do perfil (" + safeLabel(candidateSeniority) + ") alinhada ao nível da vaga (" + safeLabel(jobSeniority) + ").");
            return 20;
        }
        if (candRank > jobRank) {
            positivePoints.add("Senioridade do candidato (" + safeLabel(candidateSeniority) + ") atende com folga ao nível exigido (" + safeLabel(jobSeniority) + ").");
            return 18;
        }
        if (jobRank - candRank == 1) {
            attentionPoints.add("A vaga busca nível " + safeLabel(jobSeniority) + " enquanto seu perfil está configurado como " + safeLabel(candidateSeniority) + ".");
            return 12;
        }
        attentionPoints.add("Diferença relevante de senioridade: vaga " + safeLabel(jobSeniority) + " vs. perfil " + safeLabel(candidateSeniority) + ".");
        return 6;
    }

    private int evaluateModalityAndLocation(
            CandidateProfileEntity profile,
            JobPostingEntity job,
            List<String> positivePoints,
            List<String> attentionPoints
    ) {
        if (job.getWorkModality() == WorkModality.REMOTO) {
            positivePoints.add("Modalidade 100% remota compatível com qualquer localização.");
            return 15;
        }
        String candLoc = profile.getContactInfo() != null && profile.getContactInfo().location() != null
                ? profile.getContactInfo().location().trim()
                : "";
        String jobLoc = job.getLocation() != null ? job.getLocation().trim() : "";

        if (!candLoc.isEmpty() && !jobLoc.isEmpty()) {
            String cNorm = candLoc.toLowerCase(Locale.ROOT);
            String jNorm = jobLoc.toLowerCase(Locale.ROOT);
            String cityToken = cNorm.split("[,\\-/]")[0].trim();
            if (!cityToken.isEmpty() && jNorm.contains(cityToken)) {
                positivePoints.add("Localização compatível (" + jobLoc + ") para modalidade " + job.getWorkModality() + ".");
                return 15;
            }
            attentionPoints.add("Verifique disponibilidade presencial/híbrida em " + jobLoc + " (seu perfil indica " + candLoc + ").");
            return 9;
        }
        return 12;
    }

    private int evaluateEducationAndLanguages(
            CandidateProfileEntity profile,
            JobPostingEntity job,
            List<String> positivePoints,
            List<String> attentionPoints
    ) {
        int score = 8;
        if (profile.getEducation() != null && !profile.getEducation().isEmpty()) {
            score += 4;
            positivePoints.add("Formação acadêmica cadastrada compatível com a área de tecnologia.");
        } else {
            attentionPoints.add("Formação acadêmica não detalhada no perfil.");
        }

        if (profile.getLanguages() != null && !profile.getLanguages().isEmpty()) {
            score += 3;
        }
        return Math.min(15, score);
    }

    private String buildExplainableSummary(
            JobPostingEntity job,
            int finalScore,
            List<String> matchedTechs,
            List<String> missingTechs,
            int seniorityScore
    ) {
        String tier = finalScore >= 80 ? "Alta compatibilidade"
                : finalScore >= 60 ? "Boa compatibilidade"
                : "Compatibilidade moderada / parcial";

        StringBuilder sb = new StringBuilder();
        sb.append(tier).append(" (").append(finalScore).append("%) para a vaga de ")
                .append(job.getTitle()).append(" na ").append(job.getCompanyName()).append(". ");

        if (!matchedTechs.isEmpty()) {
            sb.append("Seu perfil atende às tecnologias centrais (").append(String.join(", ", matchedTechs)).append("). ");
        }
        if (!missingTechs.isEmpty()) {
            sb.append("Pontos de atenção incluem requisitos ou diferenciais como ").append(String.join(", ", missingTechs)).append(". ");
        }
        if (seniorityScore >= 18) {
            sb.append("Nível de senioridade aderente à oportunidade.");
        }
        return sb.toString().trim();
    }

    private Set<String> collectCandidateSkillsLower(CandidateProfileEntity profile) {
        Set<String> set = new LinkedHashSet<>();
        if (profile.getSkills() != null) {
            for (SkillItemDto s : profile.getSkills()) {
                if (s.name() != null) {
                    set.add(s.name().toLowerCase(Locale.ROOT).trim());
                }
            }
        }
        if (profile.getExperiences() != null) {
            for (ExperienceItemDto exp : profile.getExperiences()) {
                if (exp.technologies() != null) {
                    for (String t : exp.technologies()) {
                        set.add(t.toLowerCase(Locale.ROOT).trim());
                    }
                }
            }
        }
        return set;
    }

    private String buildCandidateCorpusLower(CandidateProfileEntity profile) {
        StringBuilder sb = new StringBuilder();
        if (profile.getHeadline() != null) sb.append(profile.getHeadline()).append(" ");
        if (profile.getSummary() != null) sb.append(profile.getSummary()).append(" ");
        if (profile.getSkills() != null) {
            for (SkillItemDto s : profile.getSkills()) {
                sb.append(s.name()).append(" ");
            }
        }
        if (profile.getExperiences() != null) {
            for (ExperienceItemDto e : profile.getExperiences()) {
                sb.append(e.role()).append(" ").append(e.description()).append(" ");
            }
        }
        if (profile.getEducation() != null) {
            for (EducationItemDto ed : profile.getEducation()) {
                sb.append(ed.degree()).append(" ").append(ed.institution()).append(" ");
            }
        }
        if (profile.getProjects() != null) {
            for (ProjectItemDto p : profile.getProjects()) {
                sb.append(p.name()).append(" ").append(p.description()).append(" ");
            }
        }
        if (profile.getLanguages() != null) {
            for (LanguageItemDto l : profile.getLanguages()) {
                sb.append(l.language()).append(" ").append(l.proficiency()).append(" ");
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private boolean hasKeywordOverlap(String requirementLower, String candidateCorpusLower) {
        String[] tokens = requirementLower.split("[^a-zA-Z0-9#+\\-.]+");
        for (String token : tokens) {
            if (token.length() >= 4 && !isStopWord(token) && candidateCorpusLower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStopWord(String word) {
        return Set.of(
                "para", "como", "mais", "pelo", "pela", "seus", "suas", "experiência", "experiencia",
                "conhecimento", "conhecimentos", "desenvolvimento", "trabalho", "anos", "área", "area",
                "requisitos", "desejável", "desejavel", "diferencial", "sólida", "solida", "boas", "práticas"
        ).contains(word);
    }

    private int rankSeniority(String s) {
        if (s == null) return 2;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.contains("estágio") || lower.contains("estagiário") || lower.contains("intern")) return 1;
        if (lower.contains("júnior") || lower.contains("junior") || lower.contains("jr")) return 2;
        if (lower.contains("pleno") || lower.contains("mid")) return 3;
        if (lower.contains("sênior") || lower.contains("senior") || lower.contains("sr") || lower.contains("lead") || lower.contains("especialista")) return 4;
        return 2;
    }

    private String safeLabel(String s) {
        return (s == null || s.isBlank()) ? "Júnior/Pleno" : s.trim();
    }

    public JobMatchAnalysisDto toDto(JobMatchAnalysisEntity entity) {
        return new JobMatchAnalysisDto(
                entity.getId(),
                entity.getJobId(),
                entity.getCompatibilityScore(),
                entity.getScoreBreakdown(),
                entity.getPositivePoints(),
                entity.getAttentionPoints(),
                entity.getMatchedRequirements(),
                entity.getMissingRequirements(),
                entity.getAiSummary(),
                entity.getAnalyzedAt()
        );
    }
}
