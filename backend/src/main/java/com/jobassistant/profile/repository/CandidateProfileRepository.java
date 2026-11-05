package com.jobassistant.profile.repository;

import com.jobassistant.profile.domain.CandidateProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfileEntity, UUID> {

    Optional<CandidateProfileEntity> findByUserId(UUID userId);
}
