package com.jobassistant.jobs.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPostingEntity, UUID> {

    List<JobPostingEntity> findAllByUserIdOrderByDiscoveredAtDesc(UUID userId);

    List<JobPostingEntity> findAllByUserIdAndSourceId(UUID userId, UUID sourceId);

    Optional<JobPostingEntity> findByUserIdAndFingerprintHash(UUID userId, String fingerprintHash);

    Optional<JobPostingEntity> findByIdAndUserId(UUID id, UUID userId);
}
