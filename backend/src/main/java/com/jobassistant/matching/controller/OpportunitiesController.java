package com.jobassistant.matching.controller;

import com.jobassistant.applications.domain.ApplicationEntity;
import com.jobassistant.applications.domain.ApplicationRepository;
import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.JobPostingRepository;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.service.JobDiscoveryService;
import com.jobassistant.matching.domain.JobMatchAnalysisEntity;
import com.jobassistant.matching.domain.JobMatchAnalysisRepository;
import com.jobassistant.matching.dto.JobMatchAnalysisDto;
import com.jobassistant.matching.dto.OpportunityCardDto;
import com.jobassistant.matching.service.JobMatchingEngineService;
import com.jobassistant.security.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class OpportunitiesController {

    private final JobMatchingEngineService matchingService;
    private final JobPostingRepository jobRepository;
    private final JobMatchAnalysisRepository matchRepository;
    private final ApplicationRepository applicationRepository;
    private final JobDiscoveryService discoveryService;
    private final CurrentUserProvider currentUserProvider;

    public OpportunitiesController(
            JobMatchingEngineService matchingService,
            JobPostingRepository jobRepository,
            JobMatchAnalysisRepository matchRepository,
            ApplicationRepository applicationRepository,
            JobDiscoveryService discoveryService,
            CurrentUserProvider currentUserProvider
    ) {
        this.matchingService = matchingService;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.applicationRepository = applicationRepository;
        this.discoveryService = discoveryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/jobs/{id}/match")
    public ResponseEntity<JobMatchAnalysisDto> analyzeJobMatch(@PathVariable UUID id) {
        return ResponseEntity.ok(matchingService.analyzeSingleJob(id));
    }

    @PostMapping("/jobs/match-all")
    public ResponseEntity<List<JobMatchAnalysisDto>> analyzeAllActiveJobs() {
        return ResponseEntity.ok(matchingService.analyzeAllActiveJobs());
    }

    @GetMapping("/opportunities")
    public ResponseEntity<List<OpportunityCardDto>> listOpportunities(
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String seniority,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) WorkModality modality,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobPlatform platform,
            @RequestParam(required = false) String status
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();

        List<JobPostingEntity> jobs = jobRepository.findAllByUserIdOrderByDiscoveredAtDesc(userId);
        Map<UUID, JobMatchAnalysisEntity> matchByJobId = matchRepository.findAllByUserIdOrderByCompatibilityScoreDesc(userId)
                .stream()
                .collect(Collectors.toMap(JobMatchAnalysisEntity::getJobId, Function.identity(), (a, b) -> a));
        Map<UUID, ApplicationEntity> appByJobId = applicationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(ApplicationEntity::getJobId, Function.identity(), (a, b) -> a));

        List<OpportunityCardDto> cards = jobs.stream()
                .map(job -> {
                    JobMatchAnalysisEntity match = matchByJobId.get(job.getId());
                    ApplicationEntity app = appByJobId.get(job.getId());
                    String effectiveStatus;
                    if (!job.isActive()) {
                        effectiveStatus = ApplicationStatus.VAGA_ENCERRADA.name();
                    } else if (app != null) {
                        effectiveStatus = app.getStatus().name();
                    } else if (match != null) {
                        effectiveStatus = ApplicationStatus.EM_ANALISE_IA.name();
                    } else {
                        effectiveStatus = ApplicationStatus.ENCONTRADA.name();
                    }

                    return new OpportunityCardDto(
                            discoveryService.toJobDto(job),
                            match != null ? matchingService.toDto(match) : null,
                            app != null ? app.getId() : null,
                            effectiveStatus
                    );
                })
                .filter(card -> {
                    if (minScore != null && minScore > 0) {
                        int score = card.matchAnalysis() != null ? card.matchAnalysis().compatibilityScore() : 0;
                        if (score < minScore) return false;
                    }
                    if (area != null && !area.isBlank()) {
                        String q = area.trim().toLowerCase(Locale.ROOT);
                        String text = (card.job().title() + " " + String.join(" ", card.job().technologies())).toLowerCase(Locale.ROOT);
                        if (!text.contains(q)) return false;
                    }
                    if (seniority != null && !seniority.isBlank()) {
                        String q = seniority.trim().toLowerCase(Locale.ROOT);
                        String s = card.job().seniority() != null ? card.job().seniority().toLowerCase(Locale.ROOT) : "";
                        if (!s.contains(q)) return false;
                    }
                    if (company != null && !company.isBlank()) {
                        String q = company.trim().toLowerCase(Locale.ROOT);
                        if (!card.job().companyName().toLowerCase(Locale.ROOT).contains(q)) return false;
                    }
                    if (salary != null && !salary.isBlank()) {
                        String q = salary.trim().toLowerCase(Locale.ROOT);
                        String sal = card.job().salaryRange() != null ? card.job().salaryRange().toLowerCase(Locale.ROOT) : "";
                        if (!sal.contains(q)) return false;
                    }
                    if (modality != null && card.job().workModality() != modality) {
                        return false;
                    }
                    if (location != null && !location.isBlank()) {
                        String q = location.trim().toLowerCase(Locale.ROOT);
                        String loc = card.job().location() != null ? card.job().location().toLowerCase(Locale.ROOT) : "";
                        if (!loc.contains(q)) return false;
                    }
                    if (platform != null && card.job().sourcePlatform() != platform) {
                        return false;
                    }
                    if (status != null && !status.isBlank()) {
                        if (!card.applicationStatus().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparingInt((OpportunityCardDto c) ->
                        c.matchAnalysis() != null ? c.matchAnalysis().compatibilityScore() : -1
                ).reversed())
                .toList();

        return ResponseEntity.ok(cards);
    }
}
