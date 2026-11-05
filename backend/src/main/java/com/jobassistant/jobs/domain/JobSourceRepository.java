package com.jobassistant.jobs.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobSourceRepository extends JpaRepository<JobSourceEntity, UUID> {

    List<JobSourceEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<JobSourceEntity> findByIdAndUserId(UUID id, UUID userId);
}
