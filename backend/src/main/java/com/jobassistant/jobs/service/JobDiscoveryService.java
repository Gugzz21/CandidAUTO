package com.jobassistant.jobs.service;

import com.jobassistant.jobs.connector.ConnectorFetchResult;
import com.jobassistant.jobs.connector.JobSourceConnector;
import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import com.jobassistant.jobs.discovery.DiscoveryCriteria;
import com.jobassistant.jobs.discovery.JobRelevanceFilter;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.JobSourceRepository;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.dto.ConnectorDiagnosticDto;
import com.jobassistant.jobs.dto.CreateJobSourceRequestDto;
import com.jobassistant.jobs.dto.ImportJobRequestDto;
import com.jobassistant.jobs.dto.JobPostingDto;
import com.jobassistant.jobs.dto.JobSourceDto;
import com.jobassistant.jobs.dto.SyncSourceResponseDto;
import com.jobassistant.security.CurrentUserProvider;
import com.jobassistant.shared.BadRequestException;
import com.jobassistant.shared.FileProcessingException;
import com.jobassistant.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class JobDiscoveryService {

    private final JobSourceRepository sourceRepository;
    private final JobPostingRepository jobRepository;
    private final JobNormalizationService normalizationService;
    private final JobRelevanceFilter relevanceFilter;
    private final List<JobSourceConnector> connectors;
    private final CurrentUserProvider currentUserProvider;

    public JobDiscoveryService(
            JobSourceRepository sourceRepository,
            JobPostingRepository jobRepository,
            JobNormalizationService normalizationService,
            JobRelevanceFilter relevanceFilter,
            List<JobSourceConnector> connectors,
            CurrentUserProvider currentUserProvider
    ) {
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.normalizationService = normalizationService;
        this.relevanceFilter = relevanceFilter;
        this.connectors = connectors;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<JobSourceDto> listUserSources() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return sourceRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSourceDto)
                .toList();
    }

    @Transactional
    public JobSourceDto createSource(CreateJobSourceRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        if (!request.platform().supportsAutomatedBoardSync()) {
            throw new BadRequestException(
                    "A plataforma " + request.platform() + " não possui sincronização automática de quadro por restrição de API/Termos de Uso. "
                            + "Use conectores oficiais (GREENHOUSE, LEVER, GUPY, CAREER_PAGE) ou a Importação Assistida por Link."
            );
        }
        JobSourceEntity entity = new JobSourceEntity(
                UUID.randomUUID(),
                userId,
                request.name().trim(),
                request.platform(),
                request.boardIdentifier().trim(),
                true,
                Instant.now()
        );
        entity.setLastSyncStatus("Aguardando primeira sincronização");
        return toSourceDto(sourceRepository.save(entity));
    }

    @Transactional
    public void deleteSource(UUID sourceId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        JobSourceEntity source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte de vagas não encontrada."));
        sourceRepository.delete(source);
    }

    @Transactional(noRollbackFor = {FileProcessingException.class, BadRequestException.class})
    public SyncSourceResponseDto syncSource(UUID sourceId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return syncSourceForUser(userId, sourceId, null);
    }

    @Transactional(noRollbackFor = {FileProcessingException.class, BadRequestException.class})
    public SyncSourceResponseDto syncSourceForUser(
            UUID userId,
            UUID sourceId,
            DiscoveryCriteria optionalRelevanceFilter
    ) {
        currentUserProvider.ensureUserExists(userId);
        JobSourceEntity source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte de vagas não encontrada."));

        JobSourceConnector connector = connectors.stream()
                .filter(c -> c.supports(source.getPlatform()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "A plataforma " + source.getPlatform() + " não possui sincronização automática de board. Utilize a importação assistida por link."
                ));

        ConnectorFetchResult fetchResult;
        try {
            fetchResult = connector.fetchJobsSafely(source);
        } catch (RuntimeException ex) {
            source.setLastSyncedAt(Instant.now());
            source.setLastSyncStatus(truncateStatus("Falha: " + ex.getMessage()));
            sourceRepository.save(source);
            throw ex;
        }

        List<RawDiscoveredJob> fetched = fetchResult.jobs();
        int newlyInserted = 0;
        int updatedExisting = 0;
        int filteredIrrelevant = 0;
        Map<String, Integer> discardReasons = new LinkedHashMap<>();
        Set<String> allBoardFingerprintsInSync = new HashSet<>();
        List<JobPostingDto> syncedDtos = new ArrayList<>();

        for (RawDiscoveredJob raw : fetched) {
            String fingerprint = normalizationService.computeFingerprint(
                    raw.companyName(),
                    raw.title(),
                    raw.location()
            );
            allBoardFingerprintsInSync.add(fingerprint);

            if (optionalRelevanceFilter != null) {
                JobRelevanceFilter.RelevanceDecision decision = relevanceFilter.evaluateRelevance(raw, optionalRelevanceFilter);
                if (!decision.relevant()) {
                    filteredIrrelevant++;
                    String reason = decision.discardReason() != null ? decision.discardReason() : "NAO_ADERENTE";
                    discardReasons.merge(reason, 1, Integer::sum);
                    continue;
                }
            }

            Optional<JobPostingEntity> existingOpt = jobRepository.findByUserIdAndFingerprintHash(userId, fingerprint);
            JobPostingEntity saved;
            if (existingOpt.isPresent()) {
                JobPostingEntity existing = existingOpt.get();
                existing.setSourceId(source.getId());
                normalizationService.updateExistingEntity(existing, raw);
                saved = jobRepository.save(existing);
                updatedExisting++;
            } else {
                JobPostingEntity created = normalizationService.normalizeToEntity(userId, source.getId(), raw);
                saved = jobRepository.save(created);
                newlyInserted++;
            }
            syncedDtos.add(toJobDto(saved));
        }

        // Proteção estrita contra expiração indevida:
        // Somente uma sincronização validada (SUCCESS_WITH_RESULTS ou SUCCESS_EMPTY_CONFIRMED) pode expirar vagas antigas.
        int markedExpired = 0;
        List<JobPostingEntity> previousSourceJobs = jobRepository.findAllByUserIdAndSourceId(userId, source.getId());
        long previousActiveCount = previousSourceJobs.stream().filter(JobPostingEntity::isActive).count();

        boolean validatedSyncAllowsExpiration = fetchResult.outcomeStatus() == SyncOutcomeStatus.SUCCESS_WITH_RESULTS
                || (fetchResult.outcomeStatus() == SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED && fetchResult.authoritativeEmpty());

        if (validatedSyncAllowsExpiration) {
            for (JobPostingEntity prev : previousSourceJobs) {
                if (prev.isActive() && !allBoardFingerprintsInSync.contains(prev.getFingerprintHash())) {
                    prev.setActive(false);
                    jobRepository.save(prev);
                    markedExpired++;
                }
            }
        }

        source.setLastSyncedAt(Instant.now());
        String statusMessage;
        if (fetchResult.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR) {
            statusMessage = "Falha de parsing HTML/JSON (" + fetchResult.diagnosticNote() + ")";
        } else if (fetched.isEmpty() && previousActiveCount > 0 && !validatedSyncAllowsExpiration) {
            statusMessage = "Aviso: 0 vagas extraídas (mantidas " + previousActiveCount + " vagas ativas por segurança)";
        } else if (filteredIrrelevant > 0) {
            statusMessage = "Sucesso: " + fetched.size() + " lidas (" + newlyInserted + " novas, " + updatedExisting + " atualizadas, " + filteredIrrelevant + " fora do perfil)";
        } else {
            statusMessage = "Sucesso: " + fetched.size() + " vagas lidas (" + newlyInserted + " novas, " + updatedExisting + " atualizadas, " + markedExpired + " expiradas)";
        }
        source.setLastSyncStatus(truncateStatus(statusMessage));
        JobSourceEntity savedSource = sourceRepository.save(source);

        ConnectorDiagnosticDto diagnostic = new ConnectorDiagnosticDto(
                source.getPlatform() + " (" + source.getName() + ")",
                fetchResult.queriedUrl().isBlank() ? source.getBoardIdentifier() : fetchResult.queriedUrl(),
                fetchResult.httpStatus(),
                fetchResult.outcomeStatus(),
                !fetched.isEmpty(),
                fetched.size(),
                filteredIrrelevant,
                discardReasons,
                newlyInserted,
                updatedExisting,
                fetchResult.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR ? fetchResult.diagnosticNote() : "",
                Instant.now()
        );

        return new SyncSourceResponseDto(
                toSourceDto(savedSource),
                fetched.size(),
                newlyInserted,
                updatedExisting,
                markedExpired,
                syncedDtos,
                diagnostic
        );
    }

    @Transactional
    public JobPostingDto importManualJob(ImportJobRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        RawDiscoveredJob raw = new RawDiscoveredJob(
                null,
                request.sourcePlatform(),
                request.companyName().trim(),
                request.title().trim(),
                request.descriptionRaw(),
                request.location() != null ? request.location().trim() : "",
                request.workModality(),
                request.seniority(),
                request.salaryRange(),
                List.of(),
                request.originalUrl().trim(),
                Instant.now()
        );

        String fingerprint = normalizationService.computeFingerprint(raw.companyName(), raw.title(), raw.location());
        Optional<JobPostingEntity> existingOpt = jobRepository.findByUserIdAndFingerprintHash(userId, fingerprint);

        JobPostingEntity saved;
        if (existingOpt.isPresent()) {
            JobPostingEntity existing = existingOpt.get();
            normalizationService.updateExistingEntity(existing, raw);
            saved = jobRepository.save(existing);
        } else {
            JobPostingEntity created = normalizationService.normalizeToEntity(userId, null, raw);
            saved = jobRepository.save(created);
        }
        return toJobDto(saved);
    }

    @Transactional(readOnly = true)
    public List<JobPostingDto> listUserJobs(
            JobPlatform platform,
            WorkModality modality,
            Boolean activeOnly,
            String search
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        String query = search != null ? search.trim().toLowerCase(Locale.ROOT) : "";

        return jobRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId)
                .stream()
                .filter(job -> platform == null || job.getSourcePlatform() == platform)
                .filter(job -> modality == null || job.getWorkModality() == modality)
                .filter(job -> activeOnly == null || !activeOnly || job.isActive())
                .filter(job -> {
                    if (query.isEmpty()) return true;
                    String searchable = (job.getTitle() + " " + job.getCompanyName() + " " + job.getLocation() + " "
                            + String.join(" ", job.getTechnologies())).toLowerCase(Locale.ROOT);
                    return searchable.contains(query);
                })
                .map(this::toJobDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobPostingDto getJobById(UUID jobId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        JobPostingEntity entity = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para o usuário atual."));
        return toJobDto(entity);
    }

    @Transactional
    public JobPostingDto updateJobAvailability(UUID jobId, boolean active) {
        UUID userId = currentUserProvider.getCurrentUserId();
        JobPostingEntity entity = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada para o usuário atual."));
        entity.setActive(active);
        return toJobDto(jobRepository.save(entity));
    }

    private String truncateStatus(String msg) {
        if (msg == null) return "";
        return msg.length() <= 155 ? msg : msg.substring(0, 152) + "...";
    }

    private JobSourceDto toSourceDto(JobSourceEntity entity) {
        return new JobSourceDto(
                entity.getId(),
                entity.getName(),
                entity.getPlatform(),
                entity.getBoardIdentifier(),
                entity.isEnabled(),
                entity.getLastSyncedAt(),
                entity.getLastSyncStatus(),
                entity.getCreatedAt()
        );
    }

    public JobPostingDto toJobDto(JobPostingEntity entity) {
        return new JobPostingDto(
                entity.getId(),
                entity.getSourceId(),
                entity.getExternalId(),
                entity.getSourcePlatform(),
                entity.getCompanyName(),
                entity.getTitle(),
                entity.getDescriptionRaw(),
                entity.getMandatoryRequirements(),
                entity.getDesirableRequirements(),
                entity.getTechnologies(),
                entity.getSeniority(),
                entity.getWorkModality(),
                entity.getLocation(),
                entity.getSalaryRange(),
                entity.getBenefits(),
                entity.getApplicationQuestions(),
                entity.getOriginalUrl(),
                entity.getPublishedAt(),
                entity.getDiscoveredAt(),
                entity.isActive()
        );
    }
}
