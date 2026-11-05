package com.jobassistant.profile.service;

import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.domain.ProfileEvidenceEntity;
import com.jobassistant.profile.dto.CandidateProfileDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.ProfileEvidenceDto;
import com.jobassistant.profile.dto.UpdateProfileRequestDto;
import com.jobassistant.profile.repository.CandidateProfileRepository;
import com.jobassistant.profile.repository.ProfileEvidenceRepository;
import com.jobassistant.resume.domain.ResumeEntity;
import com.jobassistant.resume.domain.ResumeRepository;
import com.jobassistant.resume.domain.ResumeStatus;
import com.jobassistant.security.CurrentUserProvider;
import com.jobassistant.shared.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private final CandidateProfileRepository profileRepository;
    private final ProfileEvidenceRepository evidenceRepository;
    private final ResumeRepository resumeRepository;
    private final CurrentUserProvider currentUserProvider;

    public ProfileService(
            CandidateProfileRepository profileRepository,
            ProfileEvidenceRepository evidenceRepository,
            ResumeRepository resumeRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository;
        this.resumeRepository = resumeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public CandidateProfileDto getCurrentUserProfile() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return profileRepository.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(() -> emptyProfileDto(userId));
    }

    @Transactional
    public CandidateProfileDto updateCurrentUserProfile(UpdateProfileRequestDto request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new CandidateProfileEntity(UUID.randomUUID(), userId));

        profile.setFullName(request.fullName() != null ? request.fullName().trim() : "");
        profile.setHeadline(request.headline() != null ? request.headline().trim() : "");
        profile.setSeniorityLevel(request.seniorityLevel() != null ? request.seniorityLevel().trim() : "");
        profile.setSummary(request.summary() != null ? request.summary().trim() : "");
        profile.setContactInfo(request.contactInfo() != null ? request.contactInfo() : ContactInfoDto.empty());
        profile.setExperiences(request.experiences() != null ? request.experiences() : List.of());
        profile.setEducation(request.education() != null ? request.education() : List.of());
        profile.setSkills(request.skills() != null ? request.skills() : List.of());
        profile.setLanguages(request.languages() != null ? request.languages() : List.of());
        profile.setCertifications(request.certifications() != null ? request.certifications() : List.of());
        profile.setProjects(request.projects() != null ? request.projects() : List.of());
        profile.setTargetRoles(request.targetRoles() != null ? request.targetRoles() : List.of());
        profile.setConfirmed(false);
        profile.setConfirmedAt(null);
        profile.setUpdatedAt(Instant.now());

        CandidateProfileEntity saved = profileRepository.save(profile);
        return toDto(saved);
    }

    @Transactional
    public CandidateProfileDto confirmCurrentUserProfile() {
        UUID userId = currentUserProvider.getCurrentUserId();
        CandidateProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Nenhum perfil profissional cadastrado para confirmação. Importe um currículo ou salve o perfil primeiro."));

        if (profile.getFullName() == null || profile.getFullName().isBlank()) {
            throw new BadRequestException("O nome completo do candidato deve estar preenchido antes de confirmar o perfil.");
        }

        profile.setConfirmed(true);
        profile.setConfirmedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        CandidateProfileEntity saved = profileRepository.save(profile);

        List<ProfileEvidenceEntity> evidences = evidenceRepository.findAllByProfileIdOrderByCreatedAtAsc(saved.getId());
        for (ProfileEvidenceEntity ev : evidences) {
            ev.setNeedsReview(false);
        }
        evidenceRepository.saveAll(evidences);

        if (saved.getResumeId() != null) {
            resumeRepository.findByIdAndUserId(saved.getResumeId(), userId).ifPresent(resume -> {
                resume.setStatus(ResumeStatus.CONFIRMED);
                resumeRepository.save(resume);
            });
        }

        return toDto(saved);
    }

    public CandidateProfileDto toDto(CandidateProfileEntity entity) {
        List<ProfileEvidenceDto> evidenceDtos = evidenceRepository
                .findAllByProfileIdOrderByCreatedAtAsc(entity.getId())
                .stream()
                .map(ev -> new ProfileEvidenceDto(
                        ev.getId(),
                        ev.getFieldPath(),
                        ev.getFieldLabel(),
                        ev.getExtractedValue(),
                        ev.getEvidenceText(),
                        ev.isVerified(),
                        ev.isNeedsReview()
                ))
                .toList();

        return new CandidateProfileDto(
                entity.getId(),
                entity.getUserId(),
                entity.getResumeId(),
                entity.getFullName(),
                entity.getHeadline(),
                entity.getSeniorityLevel(),
                entity.getSummary(),
                entity.getContactInfo(),
                entity.getExperiences(),
                entity.getEducation(),
                entity.getSkills(),
                entity.getLanguages(),
                entity.getCertifications(),
                entity.getProjects(),
                entity.getTargetRoles(),
                evidenceDtos,
                entity.isConfirmed(),
                entity.getConfirmedAt(),
                entity.getUpdatedAt()
        );
    }

    private CandidateProfileDto emptyProfileDto(UUID userId) {
        return new CandidateProfileDto(
                null,
                userId,
                null,
                "",
                "",
                "",
                "",
                ContactInfoDto.empty(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                null,
                null
        );
    }
}
