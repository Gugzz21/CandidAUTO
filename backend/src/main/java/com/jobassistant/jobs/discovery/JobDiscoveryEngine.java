package com.jobassistant.jobs.discovery;

import com.jobassistant.automation.domain.SearchConfigEntity;
import com.jobassistant.automation.domain.SearchConfigRepository;
import com.jobassistant.jobs.connector.ConnectorFetchResult;
import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.JobSourceRepository;
import com.jobassistant.jobs.dto.AutomatedDiscoveryResponseDto;
import com.jobassistant.jobs.dto.ConnectorDiagnosticDto;
import com.jobassistant.jobs.dto.JobPostingDto;
import com.jobassistant.jobs.dto.SyncSourceResponseDto;
import com.jobassistant.jobs.service.JobDiscoveryService;
import com.jobassistant.jobs.service.JobNormalizationService;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.dto.SkillItemDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Motor central de Descoberta Automática de Vagas (Job Discovery Engine).
 * Registra telemetria reproduzível por consulta/fonte: URL consultada, código HTTP,
 * vagas interpretadas pelo parser, motivos de descarte e vagas salvas/atualizadas.
 */
@Service
public class JobDiscoveryEngine {

    private final JobDiscoveryService jobDiscoveryService;
    private final JobSourceRepository sourceRepository;
    private final JobPostingRepository jobRepository;
    private final JobNormalizationService normalizationService;
    private final JobRelevanceFilter relevanceFilter;
    private final List<QueryJobSearchProvider> searchProviders;
    private final SearchConfigRepository searchConfigRepository;
    private final CandidateProfileRepository profileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final boolean openWebDiscoveryEnabled;

    public JobDiscoveryEngine(
            JobDiscoveryService jobDiscoveryService,
            JobSourceRepository sourceRepository,
            JobPostingRepository jobRepository,
            JobNormalizationService normalizationService,
            JobRelevanceFilter relevanceFilter,
            List<QueryJobSearchProvider> searchProviders,
            SearchConfigRepository searchConfigRepository,
            CandidateProfileRepository profileRepository,
            CurrentUserProvider currentUserProvider,
            @Value("${app.discovery.open-web-enabled:true}") boolean openWebDiscoveryEnabled
    ) {
        this.jobDiscoveryService = jobDiscoveryService;
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.normalizationService = normalizationService;
        this.relevanceFilter = relevanceFilter;
        this.searchProviders = searchProviders;
        this.searchConfigRepository = searchConfigRepository;
        this.profileRepository = profileRepository;
        this.currentUserProvider = currentUserProvider;
        this.openWebDiscoveryEnabled = openWebDiscoveryEnabled;
    }

    @Transactional
    public AutomatedDiscoveryResponseDto discoverJobsForCurrentUser() {
        UUID userId = currentUserProvider.getCurrentUserId();
        SearchConfigEntity config = searchConfigRepository.findByUserId(userId)
                .orElseGet(() -> searchConfigRepository.save(new SearchConfigEntity(UUID.randomUUID(), userId)));
        return discoverJobsForUser(userId, config);
    }

    @Transactional(readOnly = true)
    public List<ConnectorDiagnosticDto> getLatestDiagnosticsForCurrentUser() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return searchConfigRepository.findByUserId(userId)
                .map(SearchConfigEntity::getLastExecutionDiagnostics)
                .orElse(List.of());
    }

    @Transactional
    public AutomatedDiscoveryResponseDto discoverJobsForUser(UUID userId, SearchConfigEntity config) {
        currentUserProvider.ensureUserExists(userId);
        DiscoveryCriteria criteria = buildCriteriaForUser(userId, config);

        int queriesExecuted = 0;
        int syncedUserSources = 0;
        int failedSourcesOrQueries = 0;
        int rawCandidatesEvaluated = 0;
        int relevantJobsMatched = 0;
        int newlyInserted = 0;
        int updatedExisting = 0;
        int filteredOutIrrelevant = 0;
        List<String> activeProviders = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ConnectorDiagnosticDto> diagnostics = new ArrayList<>();
        List<JobPostingDto> persistedDtos = new ArrayList<>();
        Set<String> seenFingerprintsInCycle = new HashSet<>();

        // 1. Sincronizar fontes explicitamente cadastradas pelo usuário (sem ocultar falhas!)
        List<JobSourceEntity> enabledUserSources = sourceRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(JobSourceEntity::isEnabled)
                .toList();

        for (JobSourceEntity src : enabledUserSources) {
            try {
                SyncSourceResponseDto syncRes = jobDiscoveryService.syncSourceForUser(userId, src.getId(), criteria);
                syncedUserSources++;
                rawCandidatesEvaluated += syncRes.totalFetched();
                relevantJobsMatched += syncRes.jobs().size();
                newlyInserted += syncRes.newlyInserted();
                updatedExisting += syncRes.updatedExisting();
                filteredOutIrrelevant += Math.max(0, syncRes.totalFetched() - syncRes.jobs().size());
                persistedDtos.addAll(syncRes.jobs());
                if (syncRes.diagnostic() != null) {
                    diagnostics.add(syncRes.diagnostic());
                }
            } catch (RuntimeException ex) {
                failedSourcesOrQueries++;
                String errMsg = "Fonte '" + src.getName() + "' (" + src.getPlatform() + "): " + ex.getMessage();
                warnings.add(errMsg);
                diagnostics.add(new ConnectorDiagnosticDto(
                        src.getPlatform() + " (" + src.getName() + ")",
                        src.getBoardIdentifier(),
                        null,
                        SyncOutcomeStatus.NETWORK_ERROR,
                        false,
                        0,
                        0,
                        Map.of(),
                        0,
                        0,
                        ex.getMessage(),
                        Instant.now()
                ));
            }
        }

        // 2. Executar Descoberta Ativa por Consultas em Provedores Públicos
        if (openWebDiscoveryEnabled) {
            for (QueryJobSearchProvider provider : searchProviders) {
                QueryJobSearchProvider.ProviderSearchResult result = provider.search(criteria);
                queriesExecuted += result.queriesExecuted();
                warnings.addAll(result.warnings());

                boolean providerHadSuccess = false;
                for (QueryJobSearchProvider.ProviderQueryExecution exec : result.executions()) {
                    ConnectorFetchResult fetch = exec.fetchResult();
                    if (fetch.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                            || fetch.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                            || fetch.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR) {
                        failedSourcesOrQueries++;
                    } else {
                        providerHadSuccess = true;
                    }

                    int execDiscarded = 0;
                    int execInserted = 0;
                    int execUpdated = 0;
                    Map<String, Integer> discardReasons = new LinkedHashMap<>();

                    for (RawDiscoveredJob raw : fetch.jobs()) {
                        rawCandidatesEvaluated++;
                        String cleanCompany = raw.companyName() != null ? raw.companyName().trim() : "";
                        String cleanTitle = raw.title() != null ? raw.title().trim() : "";
                        String cleanLocation = raw.location() != null ? raw.location().trim() : "";
                        String fingerprint = normalizationService.computeFingerprint(cleanCompany, cleanTitle, cleanLocation);

                        if (!seenFingerprintsInCycle.add(fingerprint)) {
                            execDiscarded++;
                            discardReasons.merge("DUPLICADA_NO_MESMO_CICLO", 1, Integer::sum);
                            continue;
                        }

                        JobRelevanceFilter.RelevanceDecision decision = relevanceFilter.evaluateRelevance(raw, criteria);
                        if (!decision.relevant()) {
                            filteredOutIrrelevant++;
                            execDiscarded++;
                            discardReasons.merge(
                                    decision.discardReason() != null ? decision.discardReason() : "NAO_ADERENTE",
                                    1,
                                    Integer::sum
                            );
                            continue;
                        }

                        relevantJobsMatched++;
                        Optional<JobPostingEntity> existingOpt = jobRepository.findByUserIdAndFingerprintHash(userId, fingerprint);
                        JobPostingEntity saved;
                        if (existingOpt.isPresent()) {
                            JobPostingEntity existing = existingOpt.get();
                            normalizationService.updateExistingEntity(existing, raw);
                            saved = jobRepository.save(existing);
                            updatedExisting++;
                            execUpdated++;
                        } else {
                            JobPostingEntity created = normalizationService.normalizeToEntity(userId, null, raw);
                            saved = jobRepository.save(created);
                            newlyInserted++;
                            execInserted++;
                        }
                        persistedDtos.add(jobDiscoveryService.toJobDto(saved));
                    }

                    String errDetail = (fetch.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                            || fetch.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                            || fetch.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR)
                            ? fetch.diagnosticNote()
                            : "";

                    diagnostics.add(new ConnectorDiagnosticDto(
                            exec.connectorLabel(),
                            fetch.queriedUrl(),
                            fetch.httpStatus(),
                            fetch.outcomeStatus(),
                            !fetch.jobs().isEmpty(),
                            fetch.jobs().size(),
                            execDiscarded,
                            discardReasons,
                            execInserted,
                            execUpdated,
                            errDetail,
                            Instant.now()
                    ));
                }

                if (providerHadSuccess) {
                    activeProviders.add(provider.providerName());
                }
            }
        }

        if (config != null) {
            config.setLastExecutionDiagnostics(diagnostics);
            searchConfigRepository.save(config);
        }

        return new AutomatedDiscoveryResponseDto(
                queriesExecuted,
                syncedUserSources,
                failedSourcesOrQueries,
                rawCandidatesEvaluated,
                relevantJobsMatched,
                newlyInserted,
                updatedExisting,
                filteredOutIrrelevant,
                activeProviders,
                warnings,
                diagnostics,
                persistedDtos
        );
    }

    public DiscoveryCriteria buildCriteriaForUser(UUID userId, SearchConfigEntity config) {
        Optional<CandidateProfileEntity> profileOpt = profileRepository.findByUserId(userId);

        Set<String> roles = new LinkedHashSet<>();
        if (config != null && config.getDesiredRoles() != null) {
            roles.addAll(config.getDesiredRoles());
        }
        if (profileOpt.isPresent()) {
            CandidateProfileEntity profile = profileOpt.get();
            if (profile.getTargetRoles() != null) {
                roles.addAll(profile.getTargetRoles());
            }
            if (roles.isEmpty() && profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
                roles.add(profile.getHeadline().trim());
            }
        }

        List<String> modalities = config != null && config.getDesiredModalities() != null
                ? config.getDesiredModalities()
                : List.of();

        Set<String> seniorities = new LinkedHashSet<>();
        if (config != null && config.getDesiredSeniorities() != null) {
            seniorities.addAll(config.getDesiredSeniorities());
        }
        if (seniorities.isEmpty() && profileOpt.isPresent()
                && profileOpt.get().getSeniorityLevel() != null
                && !profileOpt.get().getSeniorityLevel().isBlank()) {
            seniorities.add(profileOpt.get().getSeniorityLevel().trim());
        }

        List<String> skills = profileOpt
                .map(CandidateProfileEntity::getSkills)
                .orElse(List.of())
                .stream()
                .map(SkillItemDto::name)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        return new DiscoveryCriteria(
                new ArrayList<>(roles),
                modalities,
                new ArrayList<>(seniorities),
                skills
        );
    }
}
