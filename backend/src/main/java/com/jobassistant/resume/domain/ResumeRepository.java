package com.jobassistant.resume.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<ResumeEntity, UUID> {

    List<ResumeEntity> findAllByUserIdOrderByUploadedAtDesc(UUID userId);

    Optional<ResumeEntity> findByIdAndUserId(UUID id, UUID userId);
}
