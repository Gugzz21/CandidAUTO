package com.jobassistant.applications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_qa_vault")
public class CandidateQaVaultEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_key", nullable = false, length = 200)
    private String questionKey;

    @Column(name = "question_label", nullable = false, length = 300)
    private String questionLabel;

    @Column(name = "confirmed_answer", nullable = false, columnDefinition = "TEXT")
    private String confirmedAnswer;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CandidateQaVaultEntity() {
    }

    public CandidateQaVaultEntity(
            UUID id,
            UUID userId,
            String questionKey,
            String questionLabel,
            String confirmedAnswer
    ) {
        this.id = id;
        this.userId = userId;
        this.questionKey = questionKey;
        this.questionLabel = questionLabel;
        this.confirmedAnswer = confirmedAnswer;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQuestionKey() {
        return questionKey;
    }

    public String getQuestionLabel() {
        return questionLabel;
    }

    public void setQuestionLabel(String questionLabel) {
        this.questionLabel = questionLabel;
    }

    public String getConfirmedAnswer() {
        return confirmedAnswer;
    }

    public void setConfirmedAnswer(String confirmedAnswer) {
        this.confirmedAnswer = confirmedAnswer;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
