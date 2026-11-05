package com.jobassistant.applications.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateQaVaultRepository extends JpaRepository<CandidateQaVaultEntity, UUID> {

    List<CandidateQaVaultEntity> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<CandidateQaVaultEntity> findByUserIdAndQuestionKey(UUID userId, String questionKey);
}
