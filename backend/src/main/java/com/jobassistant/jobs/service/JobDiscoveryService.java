package com.jobassistant.jobs.service;

import com.jobassistant.jobs.connector.JobSourceConnector;
import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.JobSourceEntity;
import com.jobassistant.jobs.domain.JobSourceRepository;
import com.jobassistant.jobs.domain.WorkModality;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class JobDiscoveryService {

    private final JobSourceRepository sourceRepository;
    private final JobPostingRepository jobRepository;
    private final JobNormalizationService normalizationService;
    private final List<JobSourceConnector> connectors;
    private final CurrentUserProvider currentUserProvider;

    public JobDiscoveryService(
            JobSourceRepository sourceRepository,
            JobPostingRepository jobRepository,
            JobNormalizationService normalizationService,
            List<JobSourceConnector> connectors,
            CurrentUserProvider currentUserProvider
    ) {
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.normalizationService = normalizationService;
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
        JobSourceEntity source = sourceRepository.findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fonte de vagas não encontrada."));

        JobSourceConnector connector = connectors.stream()
                .filter(c -> c.supports(source.getPlatform()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "A plataforma " + source.getPlatform() + " não possui sincronização automática de board. Utilize a importação assistida por link."
                ));

        List<RawDiscoveredJob> fetched;
        try {
            fetched = connector.fetchJobs(source);
        } catch (RuntimeException ex) {
            source.setLastSyncedAt(Instant.now());
            source.setLastSyncStatus("Falha: " + ex.getMessage());
            sourceRepository.save(source);
            throw ex;
        }

        int newlyInserted = 0;
        int updatedExisting = 0;
        Set<String> activeFingerprintsInSync = new HashSet<>();
        List<JobPostingDto> syncedDtos = new ArrayList<>();

        for (RawDiscoveredJob raw : fetched) {
            String fingerprint = normalizationService.computeFingerprint(
                    raw.companyName(),
                    raw.title(),
                    raw.location()
            );
            activeFingerprintsInSync.add(fingerprint);

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

        // Identificar oportunidades expiradas/removidas do board
        int markedExpired = 0;
        List<JobPostingEntity> previousSourceJobs = jobRepository.findAllByUserIdAndSourceId(userId, source.getId());
        for (JobPostingEntity prev : previousSourceJobs) {
            if (prev.isActive() && !activeFingerprintsInSync.contains(prev.getFingerprintHash())) {
                prev.setActive(false);
                jobRepository.save(prev);
                markedExpired++;
            }
        }

        source.setLastSyncedAt(Instant.now());
        source.setLastSyncStatus(
                "Sucesso: " + fetched.size() + " vagas lidas (" + newlyInserted + " novas, " + updatedExisting + " atualizadas, " + markedExpired + " expiradas)"
        );
        JobSourceEntity savedSource = sourceRepository.save(source);

        return new SyncSourceResponseDto(
                toSourceDto(savedSource),
                fetched.size(),
                newlyInserted,
                updatedExisting,
                markedExpired,
                syncedDtos
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
