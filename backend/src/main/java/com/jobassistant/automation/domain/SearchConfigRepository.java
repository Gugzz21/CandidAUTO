package com.jobassistant.automation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchConfigRepository extends JpaRepository<SearchConfigEntity, UUID> {
    Optional<SearchConfigEntity> findByUserId(UUID userId);

    List<SearchConfigEntity> findAllByAutoMonitoringEnabledTrue();
}
