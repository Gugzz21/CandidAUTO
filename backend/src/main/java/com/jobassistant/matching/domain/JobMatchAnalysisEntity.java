package com.jobassistant.matching.domain;

import com.jobassistant.matching.dto.ScoreBreakdownDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_match_analyses")
public class JobMatchAnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "compatibility_score", nullable = false)
    private int compatibilityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown", nullable = false, columnDefinition = "jsonb")
    private ScoreBreakdownDto scoreBreakdown = ScoreBreakdownDto.empty();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positive_points", nullable = false, columnDefinition = "jsonb")
    private List<String> positivePoints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attention_points", nullable = false, columnDefinition = "jsonb")
    private List<String> attentionPoints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_requirements", nullable = false, columnDefinition = "jsonb")
    private List<String> matchedRequirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_requirements", nullable = false, columnDefinition = "jsonb")
    private List<String> missingRequirements = new ArrayList<>();

    @Column(name = "ai_summary", nullable = false, columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt = Instant.now();

    protected JobMatchAnalysisEntity() {
    }

    public JobMatchAnalysisEntity(UUID id, UUID userId, UUID jobId, UUID profileId) {
        this.id = id;
        this.userId = userId;
        this.jobId = jobId;
        this.profileId = profileId;
        this.analyzedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(int compatibilityScore) {
        this.compatibilityScore = Math.max(0, Math.min(100, compatibilityScore));
    }

    public ScoreBreakdownDto getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(ScoreBreakdownDto scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown != null ? scoreBreakdown : ScoreBreakdownDto.empty();
    }

    public List<String> getPositivePoints() {
        return positivePoints;
    }

    public void setPositivePoints(List<String> positivePoints) {
        this.positivePoints = positivePoints != null ? positivePoints : new ArrayList<>();
    }

    public List<String> getAttentionPoints() {
        return attentionPoints;
    }

    public void setAttentionPoints(List<String> attentionPoints) {
        this.attentionPoints = attentionPoints != null ? attentionPoints : new ArrayList<>();
    }

    public List<String> getMatchedRequirements() {
        return matchedRequirements;
    }

    public void setMatchedRequirements(List<String> matchedRequirements) {
        this.matchedRequirements = matchedRequirements != null ? matchedRequirements : new ArrayList<>();
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }

    public void setMissingRequirements(List<String> missingRequirements) {
        this.missingRequirements = missingRequirements != null ? missingRequirements : new ArrayList<>();
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary != null ? aiSummary : "";
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
