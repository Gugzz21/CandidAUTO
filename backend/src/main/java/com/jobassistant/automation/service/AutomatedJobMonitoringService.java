package com.jobassistant.automation.service;

import com.jobassistant.applications.domain.ApplicationEntity;
import com.jobassistant.applications.domain.ApplicationRepository;
import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.automation.domain.SearchConfigEntity;
import com.jobassistant.automation.domain.SearchConfigRepository;
import com.jobassistant.automation.dto.DashboardMetricsDto;
import com.jobassistant.automation.dto.SearchConfigDto;
import com.jobassistant.automation.dto.UpdateSearchConfigRequestDto;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import com.jobassistant.jobs.discovery.JobDiscoveryEngine;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.dto.AutomatedDiscoveryResponseDto;
import com.jobassistant.jobs.dto.ConnectorDiagnosticDto;
import com.jobassistant.matching.domain.JobMatchAnalysisEntity;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.matching.service.JobMatchingEngineService;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AutomatedJobMonitoringService {

    private final SearchConfigRepository searchConfigRepository;
    private final JobPostingRepository jobRepository;
    private final JobMatchAnalysisRepository matchRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobDiscoveryEngine jobDiscoveryEngine;
    private final JobMatchingEngineService matchingService;
    private final CurrentUserProvider currentUserProvider;

    public AutomatedJobMonitoringService(
            SearchConfigRepository searchConfigRepository,
            JobPostingRepository jobRepository,
            JobMatchAnalysisRepository matchRepository,
            ApplicationRepository applicationRepository,
            CandidateProfileRepository profileRepository,
            JobDiscoveryEngine jobDiscoveryEngine,
            JobMatchingEngineService matchingService,
            CurrentUserProvider currentUserProvider
    ) {
        this.searchConfigRepository = searchConfigRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.jobDiscoveryEngine = jobDiscoveryEngine;
        this.matchingService = matchingService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public SearchConfigDto getOrCreateSearchConfig() {
        UUID userId = currentUserProvider.getCurrentUserId();
        SearchConfigEntity entity = searchConfigRepository.findByUserId(userId)
                .orElseGet(() -> searchConfigRepository.save(new SearchConfigEntity(UUID.randomUUID(), userId)));
        return toDto(entity);
    }

    @Transactional
    public SearchConfigDto updateSearchConfig(UpdateSearchConfigRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        SearchConfigEntity entity = searchConfigRepository.findByUserId(userId)
                .orElseGet(() -> new SearchConfigEntity(UUID.randomUUID(), userId));

        if (request.desiredRoles() != null) {
            entity.setDesiredRoles(request.desiredRoles());
        }
        if (request.desiredModalities() != null) {
            entity.setDesiredModalities(request.desiredModalities());
        }
        if (request.desiredSeniorities() != null) {
            entity.setDesiredSeniorities(request.desiredSeniorities());
        }
        entity.setMinCompatibilityScore(request.minCompatibilityScore());
        entity.setAutoMonitoringEnabled(request.autoMonitoringEnabled());
        entity.setIntervalMinutes(request.intervalMinutes());

        return toDto(searchConfigRepository.save(entity));
    }

    @Transactional
    public SearchConfigDto runMonitoringCycleNow() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return runMonitoringCycleForUser(userId);
    }

    @Transactional
    public SearchConfigDto runMonitoringCycleForUser(UUID userId) {
        currentUserProvider.ensureUserExists(userId);
        SearchConfigEntity config = searchConfigRepository.findByUserId(userId)
                .orElseGet(() -> searchConfigRepository.save(new SearchConfigEntity(UUID.randomUUID(), userId)));

        // 1. Executar Job Discovery Engine com telemetria completa por conector/consulta
        AutomatedDiscoveryResponseDto discovery = jobDiscoveryEngine.discoverJobsForUser(userId, config);

        Set<UUID> touchedJobIdsInCycle = discovery.discoveredJobs().stream()
                .map(j -> j.id())
                .collect(Collectors.toCollection(HashSet::new));

        // 2. Avaliar compatibilidade de vagas novas e atualizadas (quando perfil estiver confirmado)
        Optional<CandidateProfileEntity> profileOpt = profileRepository.findByUserId(userId);
        int newlyOrReanalyzed = 0;
        int highMatchCount = 0;
        int skippedAlreadyAppliedOrUnchanged = 0;
        boolean profileConfirmed = profileOpt.isPresent() && profileOpt.get().isConfirmed();

        if (profileConfirmed) {
            CandidateProfileEntity profile = profileOpt.get();
            Set<UUID> appliedJobIds = applicationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                    .stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.CANDIDATURA_ENVIADA
                            || a.getStatus() == ApplicationStatus.EM_ANDAMENTO
                            || a.getStatus() == ApplicationStatus.ENTREVISTA_AGENDADA
                            || a.getStatus() == ApplicationStatus.APROVADO)
                    .map(ApplicationEntity::getJobId)
                    .collect(Collectors.toSet());

            List<JobPostingEntity> activeJobs = jobRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId)
                    .stream()
                    .filter(JobPostingEntity::isActive)
                    .toList();

            for (JobPostingEntity job : activeJobs) {
                if (appliedJobIds.contains(job.getId())) {
                    skippedAlreadyAppliedOrUnchanged++;
                    continue;
                }

                Optional<JobMatchAnalysisEntity> existingMatch = matchRepository.findByUserIdAndJobId(userId, job.getId());
                boolean profileUpdatedAfterMatch = existingMatch.isPresent()
                        && profile.getUpdatedAt() != null
                        && existingMatch.get().getAnalyzedAt() != null
                        && profile.getUpdatedAt().isAfter(existingMatch.get().getAnalyzedAt());
                boolean shouldAnalyze = existingMatch.isEmpty() || profileUpdatedAfterMatch
                        || (touchedJobIdsInCycle.contains(job.getId()) && discovery.updatedExisting() > 0 && existingMatch.isEmpty());

                if (!shouldAnalyze) {
                    skippedAlreadyAppliedOrUnchanged++;
                    if (existingMatch.get().getCompatibilityScore() >= config.getMinCompatibilityScore()) {
                        highMatchCount++;
                    }
                    continue;
                }

                JobMatchAnalysisEntity analysis = matchingService.computeAndSaveMatch(userId, profile, job);
                newlyOrReanalyzed++;
                if (analysis.getCompatibilityScore() >= config.getMinCompatibilityScore()) {
                    highMatchCount++;
                }
            }
        }

        List<ConnectorDiagnosticDto> diags = discovery.diagnostics();
        long successWithResults = diags.stream().filter(d -> d.outcomeStatus() == SyncOutcomeStatus.SUCCESS_WITH_RESULTS).count();
        long emptyResults = diags.stream().filter(d -> d.outcomeStatus() == SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED
                || d.outcomeStatus() == SyncOutcomeStatus.EXTRACTION_INCONCLUSIVE).count();
        long failedResults = diags.stream().filter(d -> d.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                || d.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                || d.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR).count();

        config.setLastRunAt(Instant.now());
        config.setLastExecutionDiagnostics(diags);

        StringBuilder summary = new StringBuilder();
        summary.append("Ciclo concluído [")
                .append(successWithResults).append(" fonte(s)/consulta(s) com vagas, ")
                .append(emptyResults).append(" sem resultados, ")
                .append(failedResults).append(" com falha]: ")
                .append(discovery.newlyInserted()).append(" nova(s) vaga(s) relevante(s), ")
                .append(discovery.updatedExisting()).append(" atualizada(s), ")
                .append(discovery.filteredOutIrrelevant()).append(" descartada(s) pelos filtros. ");

        if (profileConfirmed) {
            summary.append(newlyOrReanalyzed).append(" vaga(s) analisada(s) pela IA, ")
                    .append(highMatchCount).append(" com compatibilidade >= ")
                    .append(config.getMinCompatibilityScore()).append("% (")
                    .append(skippedAlreadyAppliedOrUnchanged).append(" inalteradas/candidatadas mantidas).");
        } else {
            summary.append("Confirme seu Perfil Profissional na aba 'Validação' para calcular a compatibilidade.");
        }

        if (!discovery.warnings().isEmpty()) {
            summary.append(" Falhas registradas: ")
                    .append(String.join(" | ", discovery.warnings().stream().limit(3).toList()));
        }

        config.setLastRunSummary(summary.toString());
        return toDto(searchConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics() {
        UUID userId = currentUserProvider.getCurrentUserId();

        List<JobPostingEntity> jobs = jobRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId);
        List<JobMatchAnalysisEntity> matches = matchRepository.findAllByUserIdOrderByCompatibilityScoreDesc(userId);
        List<ApplicationEntity> applications = applicationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);

        int threshold = searchConfigRepository.findByUserId(userId)
                .map(SearchConfigEntity::getMinCompatibilityScore)
                .orElse(70);

        long totalJobsFound = jobs.size();
        long jobsAnalyzedByAi = matches.size();
        long highCompatibilityJobs = matches.stream()
                .filter(m -> m.getCompatibilityScore() >= threshold)
                .count();

        long applicationsPrepared = applications.size();
        long applicationsSubmitted = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.CANDIDATURA_ENVIADA
                        || a.getStatus() == ApplicationStatus.EM_ANDAMENTO
                        || a.getStatus() == ApplicationStatus.ENTREVISTA_AGENDADA
                        || a.getStatus() == ApplicationStatus.APROVADO
                        || a.getStatus() == ApplicationStatus.REPROVADO)
                .count();

        long interviewsScheduled = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.ENTREVISTA_AGENDADA
                        || a.getStatus() == ApplicationStatus.APROVADO)
                .count();

        double conversionRatePercent = totalJobsFound > 0
                ? Math.round((applicationsSubmitted * 1000.0) / totalJobsFound) / 10.0
                : 0.0;

        return new DashboardMetricsDto(
                totalJobsFound,
                jobsAnalyzedByAi,
                highCompatibilityJobs,
                applicationsPrepared,
                applicationsSubmitted,
                interviewsScheduled,
                conversionRatePercent
        );
    }

    private SearchConfigDto toDto(SearchConfigEntity entity) {
        return new SearchConfigDto(
                entity.getId(),
                entity.getDesiredRoles(),
                entity.getDesiredModalities(),
                entity.getDesiredSeniorities(),
                entity.getMinCompatibilityScore(),
                entity.isAutoMonitoringEnabled(),
                entity.getIntervalMinutes(),
                entity.getLastRunAt(),
                entity.getLastRunSummary(),
                entity.getLastExecutionDiagnostics(),
                entity.getUpdatedAt()
        );
    }
}
