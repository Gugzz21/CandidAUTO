package com.jobassistant.profile.repository;

import com.jobassistant.profile.domain.ProfileEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileEvidenceRepository extends JpaRepository<ProfileEvidenceEntity, UUID> {

    List<ProfileEvidenceEntity> findAllByProfileIdOrderByCreatedAtAsc(UUID profileId);

    void deleteAllByProfileId(UUID profileId);
}
