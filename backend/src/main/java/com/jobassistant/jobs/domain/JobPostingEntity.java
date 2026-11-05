package com.jobassistant.jobs.domain;

import com.jobassistant.jobs.dto.ApplicationQuestionDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_postings")
public class JobPostingEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "fingerprint_hash", nullable = false, length = 128)
    private String fingerprintHash;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", nullable = false, length = 40)
    private JobPlatform sourcePlatform;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "description_raw", nullable = false, columnDefinition = "TEXT")
    private String descriptionRaw;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mandatory_requirements", nullable = false, columnDefinition = "jsonb")
    private List<String> mandatoryRequirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desirable_requirements", nullable = false, columnDefinition = "jsonb")
    private List<String> desirableRequirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> technologies = new ArrayList<>();

    @Column(length = 60)
    private String seniority;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_modality", nullable = false, length = 40)
    private WorkModality workModality = WorkModality.NAO_INFORMADO;

    @Column(length = 255)
    private String location;

    @Column(name = "salary_range", length = 160)
    private String salaryRange;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> benefits = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "application_questions", nullable = false, columnDefinition = "jsonb")
    private List<ApplicationQuestionDto> applicationQuestions = new ArrayList<>();

    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;

    protected JobPostingEntity() {
    }

    public JobPostingEntity(
            UUID id,
            UUID userId,
            UUID sourceId,
            String fingerprintHash,
            String externalId,
            JobPlatform sourcePlatform,
            String companyName,
            String title,
            String descriptionRaw,
            String originalUrl
    ) {
        this.id = id;
        this.userId = userId;
        this.sourceId = sourceId;
        this.fingerprintHash = fingerprintHash;
        this.externalId = externalId;
        this.sourcePlatform = sourcePlatform;
        this.companyName = companyName;
        this.title = title;
        this.descriptionRaw = descriptionRaw;
        this.originalUrl = originalUrl;
        this.discoveredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getFingerprintHash() {
        return fingerprintHash;
    }

    public String getExternalId() {
        return externalId;
    }

    public JobPlatform getSourcePlatform() {
        return sourcePlatform;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionRaw() {
        return descriptionRaw;
    }

    public void setDescriptionRaw(String descriptionRaw) {
        this.descriptionRaw = descriptionRaw;
    }

    public List<String> getMandatoryRequirements() {
        return mandatoryRequirements;
    }

    public void setMandatoryRequirements(List<String> mandatoryRequirements) {
        this.mandatoryRequirements = mandatoryRequirements != null ? mandatoryRequirements : new ArrayList<>();
    }

    public List<String> getDesirableRequirements() {
        return desirableRequirements;
    }

    public void setDesirableRequirements(List<String> desirableRequirements) {
        this.desirableRequirements = desirableRequirements != null ? desirableRequirements : new ArrayList<>();
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies != null ? technologies : new ArrayList<>();
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public WorkModality getWorkModality() {
        return workModality;
    }

    public void setWorkModality(WorkModality workModality) {
        this.workModality = workModality != null ? workModality : WorkModality.NAO_INFORMADO;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits != null ? benefits : new ArrayList<>();
    }

    public List<ApplicationQuestionDto> getApplicationQuestions() {
        return applicationQuestions;
    }

    public void setApplicationQuestions(List<ApplicationQuestionDto> applicationQuestions) {
        this.applicationQuestions = applicationQuestions != null ? applicationQuestions : new ArrayList<>();
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
