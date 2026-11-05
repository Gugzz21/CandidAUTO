package com.jobassistant.jobs.controller;

import com.jobassistant.jobs.discovery.JobDiscoveryEngine;
import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.WorkModality;
import com.jobassistant.jobs.dto.AutomatedDiscoveryResponseDto;
import com.jobassistant.jobs.dto.ConnectorDiagnosticDto;
import com.jobassistant.jobs.dto.CreateJobSourceRequestDto;
import com.jobassistant.jobs.dto.ImportJobRequestDto;
import com.jobassistant.jobs.dto.JobPostingDto;
import com.jobassistant.jobs.dto.JobSourceDto;
import com.jobassistant.jobs.dto.SyncSourceResponseDto;
import com.jobassistant.jobs.service.JobDiscoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class JobDiscoveryController {

    private final JobDiscoveryService discoveryService;
    private final JobDiscoveryEngine discoveryEngine;

    public JobDiscoveryController(
            JobDiscoveryService discoveryService,
            JobDiscoveryEngine discoveryEngine
    ) {
        this.discoveryService = discoveryService;
        this.discoveryEngine = discoveryEngine;
    }

    @GetMapping("/job-platforms")
    public ResponseEntity<List<Map<String, Object>>> listPlatformsCapabilities() {
        List<Map<String, Object>> platforms = Arrays.stream(JobPlatform.values())
                .map(p -> Map.<String, Object>of(
                        "platform", p.name(),
                        "supportsAutomatedBoardSync", p.supportsAutomatedBoardSync(),
                        "supportsPublicQueryDiscovery", p.supportsPublicQueryDiscovery(),
                        "supportsAssistedImport", true
                ))
                .toList();
        return ResponseEntity.ok(platforms);
    }

    @PostMapping("/jobs/discover")
    public ResponseEntity<AutomatedDiscoveryResponseDto> runAutomatedDiscoveryNow() {
        return ResponseEntity.ok(discoveryEngine.discoverJobsForCurrentUser());
    }

    @GetMapping("/jobs/diagnostics")
    public ResponseEntity<List<ConnectorDiagnosticDto>> getLatestDiscoveryDiagnostics() {
        return ResponseEntity.ok(discoveryEngine.getLatestDiagnosticsForCurrentUser());
    }

    @GetMapping("/job-sources")
    public ResponseEntity<List<JobSourceDto>> listSources() {
        return ResponseEntity.ok(discoveryService.listUserSources());
    }

    @PostMapping("/job-sources")
    public ResponseEntity<JobSourceDto> createSource(@Valid @RequestBody CreateJobSourceRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discoveryService.createSource(request));
    }

    @DeleteMapping("/job-sources/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable UUID id) {
        discoveryService.deleteSource(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/job-sources/{id}/sync")
    public ResponseEntity<SyncSourceResponseDto> syncSource(@PathVariable UUID id) {
        return ResponseEntity.ok(discoveryService.syncSource(id));
    }

    @PostMapping("/jobs/import")
    public ResponseEntity<JobPostingDto> importJob(@Valid @RequestBody ImportJobRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discoveryService.importManualJob(request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobPostingDto>> listJobs(
            @RequestParam(required = false) JobPlatform platform,
            @RequestParam(required = false) WorkModality modality,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(discoveryService.listUserJobs(platform, modality, activeOnly, search));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobPostingDto> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(discoveryService.getJobById(id));
    }

    @PatchMapping("/jobs/{id}/availability")
    public ResponseEntity<JobPostingDto> updateAvailability(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(discoveryService.updateJobAvailability(id, active));
    }
}
