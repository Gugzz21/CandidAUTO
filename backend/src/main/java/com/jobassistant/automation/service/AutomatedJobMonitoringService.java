package com.jobassistant.automation.service;

import com.jobassistant.applications.domain.ApplicationEntity;
import com.jobassistant.applications.domain.ApplicationRepository;
import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.automation.domain.SearchConfigEntity;
import com.jobassistant.automation.domain.SearchConfigRepository;
import com.jobassistant.automation.dto.DashboardMetricsDto;
import com.jobassistant.automation.dto.SearchConfigDto;
import com.jobassistant.automation.dto.UpdateSearchConfigRequestDto;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.JobSourceRepository;
import com.jobassistant.jobs.service.JobDiscoveryService;
import com.jobassistant.matching.domain.JobMatchAnalysisEntity;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.matching.service.JobMatchingEngineService;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AutomatedJobMonitoringService {

    private final SearchConfigRepository searchConfigRepository;
    private final JobSourceRepository sourceRepository;
    private final JobPostingRepository jobRepository;
    private final JobMatchAnalysisRepository matchRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobDiscoveryService discoveryService;
    private final JobMatchingEngineService matchingService;
    private final CurrentUserProvider currentUserProvider;

    public AutomatedJobMonitoringService(
            SearchConfigRepository searchConfigRepository,
            JobSourceRepository sourceRepository,
            JobPostingRepository jobRepository,
            JobMatchAnalysisRepository matchRepository,
            ApplicationRepository applicationRepository,
            CandidateProfileRepository profileRepository,
            JobDiscoveryService discoveryService,
            JobMatchingEngineService matchingService,
            CurrentUserProvider currentUserProvider
    ) {
        this.searchConfigRepository = searchConfigRepository;
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
        this.discoveryService = discoveryService;
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
        SearchConfigEntity config = searchConfigRepository.findByUserId(userId)
                .orElseGet(() -> searchConfigRepository.save(new SearchConfigEntity(UUID.randomUUID(), userId)));

        List<JobSourceEntity> enabledSources = sourceRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(JobSourceEntity::isEnabled)
                .toList();

        int syncedSources = 0;
        for (JobSourceEntity src : enabledSources) {
            try {
                discoveryService.syncSource(src.getId());
                syncedSources++;
            } catch (RuntimeException ignored) {
                // Fontes que falharem na rede não interrompem a análise das vagas já descobertas
            }
        }

        Optional<CandidateProfileEntity> profileOpt = profileRepository.findByUserId(userId);
        int newlyAnalyzed = 0;
        int highMatchCount = 0;
        int skippedAlreadyAppliedOrAnalyzed = 0;

        if (profileOpt.isPresent()) {
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
                    skippedAlreadyAppliedOrAnalyzed++;
                    continue;
                }
                Optional<JobMatchAnalysisEntity> existingMatch = matchRepository.findByUserIdAndJobId(userId, job.getId());
                if (existingMatch.isPresent()) {
                    skippedAlreadyAppliedOrAnalyzed++;
                    if (existingMatch.get().getCompatibilityScore() >= config.getMinCompatibilityScore()) {
                        highMatchCount++;
                    }
                    continue;
                }

                JobMatchAnalysisEntity analysis = matchingService.computeAndSaveMatch(userId, profile, job);
                newlyAnalyzed++;
                if (analysis.getCompatibilityScore() >= config.getMinCompatibilityScore()) {
                    highMatchCount++;
                }
            }
        }

        config.setLastRunAt(Instant.now());
        config.setLastRunSummary(
                "Ciclo concluído: " + syncedSources + " fonte(s) sincronizada(s), "
                        + newlyAnalyzed + " nova(s) vaga(s) analisada(s) pela IA, "
                        + highMatchCount + " oportunidade(s) com compatibilidade >= " + config.getMinCompatibilityScore() + "% "
                        + "(" + skippedAlreadyAppliedOrAnalyzed + " já analisadas/candidatadas ignoradas para evitar repetição)."
        );

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
                entity.getUpdatedAt()
        );
    }
}
