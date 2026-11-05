package com.jobassistant.automation.domain;

import com.jobassistant.jobs.dto.ConnectorDiagnosticDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "search_configs")
public class SearchConfigEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desired_roles", columnDefinition = "jsonb", nullable = false)
    private List<String> desiredRoles = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desired_modalities", columnDefinition = "jsonb", nullable = false)
    private List<String> desiredModalities = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desired_seniorities", columnDefinition = "jsonb", nullable = false)
    private List<String> desiredSeniorities = List.of();

    @Column(name = "min_compatibility_score", nullable = false)
    private int minCompatibilityScore = 70;

    @Column(name = "auto_monitoring_enabled", nullable = false)
    private boolean autoMonitoringEnabled = true;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes = 1440;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_run_summary", columnDefinition = "TEXT")
    private String lastRunSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_execution_diagnostics", columnDefinition = "jsonb", nullable = false)
    private List<ConnectorDiagnosticDto> lastExecutionDiagnostics = List.of();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SearchConfigEntity() {
    }

    public SearchConfigEntity(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.desiredRoles = List.of(
                "Desenvolvedor Front-end Júnior",
                "Desenvolvedor Back-end Júnior",
                "Desenvolvedor Full Stack Júnior"
        );
        this.desiredModalities = List.of("REMOTO", "HIBRIDO");
        this.desiredSeniorities = List.of("Júnior", "Pleno");
        this.minCompatibilityScore = 70;
        this.autoMonitoringEnabled = true;
        this.intervalMinutes = 1440;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<String> getDesiredRoles() {
        return desiredRoles;
    }

    public void setDesiredRoles(List<String> desiredRoles) {
        this.desiredRoles = desiredRoles != null ? desiredRoles : List.of();
        this.updatedAt = Instant.now();
    }

    public List<String> getDesiredModalities() {
        return desiredModalities;
    }

    public void setDesiredModalities(List<String> desiredModalities) {
        this.desiredModalities = desiredModalities != null ? desiredModalities : List.of();
        this.updatedAt = Instant.now();
    }

    public List<String> getDesiredSeniorities() {
        return desiredSeniorities;
    }

    public void setDesiredSeniorities(List<String> desiredSeniorities) {
        this.desiredSeniorities = desiredSeniorities != null ? desiredSeniorities : List.of();
        this.updatedAt = Instant.now();
    }

    public int getMinCompatibilityScore() {
        return minCompatibilityScore;
    }

    public void setMinCompatibilityScore(int minCompatibilityScore) {
        this.minCompatibilityScore = Math.max(0, Math.min(100, minCompatibilityScore));
        this.updatedAt = Instant.now();
    }

    public boolean isAutoMonitoringEnabled() {
        return autoMonitoringEnabled;
    }

    public void setAutoMonitoringEnabled(boolean autoMonitoringEnabled) {
        this.autoMonitoringEnabled = autoMonitoringEnabled;
        this.updatedAt = Instant.now();
    }

    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
        this.intervalMinutes = Math.max(15, intervalMinutes);
        this.updatedAt = Instant.now();
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
        this.updatedAt = Instant.now();
    }

    public String getLastRunSummary() {
        return lastRunSummary;
    }

    public void setLastRunSummary(String lastRunSummary) {
        this.lastRunSummary = lastRunSummary;
        this.updatedAt = Instant.now();
    }

    public List<ConnectorDiagnosticDto> getLastExecutionDiagnostics() {
        return lastExecutionDiagnostics != null ? lastExecutionDiagnostics : List.of();
    }

    public void setLastExecutionDiagnostics(List<ConnectorDiagnosticDto> lastExecutionDiagnostics) {
        this.lastExecutionDiagnostics = lastExecutionDiagnostics != null ? lastExecutionDiagnostics : List.of();
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
