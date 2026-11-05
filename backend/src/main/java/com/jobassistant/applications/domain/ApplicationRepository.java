package com.jobassistant.applications.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    List<ApplicationEntity> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<ApplicationEntity> findByUserIdAndJobId(UUID userId, UUID jobId);

    Optional<ApplicationEntity> findByIdAndUserId(UUID id, UUID userId);
}
