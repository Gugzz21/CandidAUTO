package com.jobassistant.applications.controller;

import com.jobassistant.applications.domain.ApplicationStatus;
import com.jobassistant.applications.dto.AnswerMissingQuestionsRequestDto;
import com.jobassistant.applications.dto.ApplicationDto;
import com.jobassistant.applications.dto.CancelApplicationRequestDto;
import com.jobassistant.applications.dto.QaVaultItemDto;
import com.jobassistant.applications.dto.SubmitApplicationRequestDto;
import com.jobassistant.applications.dto.UpdateApplicationStatusRequestDto;
import com.jobassistant.applications.service.SmartApplicationService;
import com.jobassistant.automation.dto.DashboardMetricsDto;
import com.jobassistant.automation.dto.SearchConfigDto;
import com.jobassistant.automation.dto.UpdateSearchConfigRequestDto;
import com.jobassistant.automation.service.AutomatedJobMonitoringService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ApplicationController {

    private final SmartApplicationService applicationService;
    private final AutomatedJobMonitoringService monitoringService;

    public ApplicationController(
            SmartApplicationService applicationService,
            AutomatedJobMonitoringService monitoringService
    ) {
        this.applicationService = applicationService;
        this.monitoringService = monitoringService;
    }

    @PostMapping("/applications/prepare/{jobId}")
    public ResponseEntity<ApplicationDto> prepareApplication(@PathVariable UUID jobId) {
        return ResponseEntity.ok(applicationService.prepareApplicationForJob(jobId));
    }

    @PutMapping("/applications/{id}/answers")
    public ResponseEntity<ApplicationDto> answerMissingQuestions(
            @PathVariable UUID id,
            @RequestBody AnswerMissingQuestionsRequestDto request
    ) {
        return ResponseEntity.ok(applicationService.answerMissingQuestions(id, request));
    }

    @PostMapping("/applications/{id}/submit")
    public ResponseEntity<ApplicationDto> submitApplication(
            @PathVariable UUID id,
            @RequestBody SubmitApplicationRequestDto request
    ) {
        return ResponseEntity.ok(applicationService.submitApplication(id, request));
    }

    @PostMapping("/applications/{id}/cancel")
    public ResponseEntity<ApplicationDto> cancelApplication(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelApplicationRequestDto request
    ) {
        return ResponseEntity.ok(applicationService.cancelApplication(id, request));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationDto> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequestDto request
    ) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationDto>> listApplications(
            @RequestParam(required = false) ApplicationStatus status
    ) {
        return ResponseEntity.ok(applicationService.listUserApplications(status));
    }

    @GetMapping("/qa-vault")
    public ResponseEntity<List<QaVaultItemDto>> listQaVault() {
        return ResponseEntity.ok(applicationService.listUserQaVault());
    }

    @GetMapping("/search-config")
    public ResponseEntity<SearchConfigDto> getSearchConfig() {
        return ResponseEntity.ok(monitoringService.getOrCreateSearchConfig());
    }

    @PutMapping("/search-config")
    public ResponseEntity<SearchConfigDto> updateSearchConfig(
            @RequestBody UpdateSearchConfigRequestDto request
    ) {
        return ResponseEntity.ok(monitoringService.updateSearchConfig(request));
    }

    @PostMapping("/search-config/run-now")
    public ResponseEntity<SearchConfigDto> runMonitoringNow() {
        return ResponseEntity.ok(monitoringService.runMonitoringCycleNow());
    }

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        return ResponseEntity.ok(monitoringService.getDashboardMetrics());
    }
}
