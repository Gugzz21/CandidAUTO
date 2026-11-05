package com.jobassistant.jobs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_sources")
public class JobSourceEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobPlatform platform;

    @Column(name = "board_identifier", nullable = false, length = 500)
    private String boardIdentifier;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_status", length = 160)
    private String lastSyncStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobSourceEntity() {
    }

    public JobSourceEntity(
            UUID id,
            UUID userId,
            String name,
            JobPlatform platform,
            String boardIdentifier,
            boolean enabled,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.platform = platform;
        this.boardIdentifier = boardIdentifier;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public JobPlatform getPlatform() {
        return platform;
    }

    public String getBoardIdentifier() {
        return boardIdentifier;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getLastSyncStatus() {
        return lastSyncStatus;
    }

    public void setLastSyncStatus(String lastSyncStatus) {
        this.lastSyncStatus = lastSyncStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
