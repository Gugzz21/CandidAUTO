package com.jobassistant.matching.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchAnalysisRepository extends JpaRepository<JobMatchAnalysisEntity, UUID> {

    List<JobMatchAnalysisEntity> findAllByUserIdOrderByCompatibilityScoreDesc(UUID userId);

    Optional<JobMatchAnalysisEntity> findByUserIdAndJobId(UUID userId, UUID jobId);
}
