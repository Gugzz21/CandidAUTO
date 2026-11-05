package com.jobassistant.resume.service;

import com.jobassistant.ai.service.AiProfileAnalysisService;
import com.jobassistant.profile.domain.CandidateProfileEntity;
import com.jobassistant.profile.service.ProfileService;
import com.jobassistant.resume.domain.ResumeEntity;
import com.jobassistant.resume.domain.ResumeRepository;
import com.jobassistant.resume.domain.ResumeStatus;
import com.jobassistant.resume.dto.ResumeAnalyzeResponseDto;
import com.jobassistant.resume.dto.ResumeResponseDto;
import com.jobassistant.resume.parser.ResumeTextExtractor;
import com.jobassistant.resume.storage.ResumeStorageService;
import com.jobassistant.security.CurrentUserProvider;
import com.jobassistant.shared.FileProcessingException;
import com.jobassistant.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeStorageService storageService;
    private final ResumeTextExtractor textExtractor;
    private final AiProfileAnalysisService aiAnalysisService;
    private final ProfileService profileService;
    private final CurrentUserProvider currentUserProvider;

    public ResumeService(
            ResumeRepository resumeRepository,
            ResumeStorageService storageService,
            ResumeTextExtractor textExtractor,
            AiProfileAnalysisService aiAnalysisService,
            ProfileService profileService,
            CurrentUserProvider currentUserProvider
    ) {
        this.resumeRepository = resumeRepository;
        this.storageService = storageService;
        this.textExtractor = textExtractor;
        this.aiAnalysisService = aiAnalysisService;
        this.profileService = profileService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(noRollbackFor = FileProcessingException.class)
    public ResumeResponseDto uploadAndExtract(MultipartFile file) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ResumeStorageService.StoredResumeFile stored = storageService.validateAndStore(file);

        ResumeEntity entity = new ResumeEntity(
                UUID.randomUUID(),
                userId,
                stored.originalFilename(),
                stored.storageKey(),
                stored.fileType(),
                stored.fileSizeBytes(),
                ResumeStatus.UPLOADED,
                Instant.now()
        );
        entity = resumeRepository.save(entity);

        entity.setStatus(ResumeStatus.EXTRACTING);
        resumeRepository.save(entity);

        try {
            String extractedText = textExtractor.extractText(stored.contentBytes(), stored.fileType());
            entity.setExtractedText(extractedText);
            entity.setStatus(ResumeStatus.EXTRACTED);
            entity.setErrorMessage(null);
            entity = resumeRepository.save(entity);
            return toDto(entity);
        } catch (FileProcessingException ex) {
            entity.setStatus(ResumeStatus.FAILED);
            entity.setErrorMessage(ex.getMessage());
            resumeRepository.save(entity);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ResumeResponseDto> listCurrentUserResumes() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return resumeRepository.findAllByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponseDto getResumeById(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ResumeEntity entity = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Currículo não encontrado para o usuário atual."));
        return toDto(entity);
    }

    @Transactional(noRollbackFor = FileProcessingException.class)
    public ResumeAnalyzeResponseDto analyzeResume(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        ResumeEntity entity = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Currículo não encontrado para o usuário atual."));

        if (entity.getExtractedText() == null || entity.getExtractedText().isBlank()) {
            entity.setStatus(ResumeStatus.FAILED);
            entity.setErrorMessage("O currículo não possui texto extraível para análise.");
            resumeRepository.save(entity);
            throw new FileProcessingException("O currículo não possui texto extraível para análise.");
        }

        entity.setStatus(ResumeStatus.ANALYZING);
        resumeRepository.save(entity);

        try {
            CandidateProfileEntity profileEntity = aiAnalysisService.analyzeAndPersistProfile(
                    userId,
                    entity.getId(),
                    entity.getExtractedText()
            );
            entity.setStatus(ResumeStatus.REVIEW_REQUIRED);
            entity.setErrorMessage(null);
            entity = resumeRepository.save(entity);

            return new ResumeAnalyzeResponseDto(
                    toDto(entity),
                    profileService.toDto(profileEntity)
            );
        } catch (FileProcessingException ex) {
            entity.setStatus(ResumeStatus.FAILED);
            entity.setErrorMessage(ex.getMessage());
            resumeRepository.save(entity);
            throw ex;
        }
    }

    private ResumeResponseDto toDto(ResumeEntity entity) {
        String text = entity.getExtractedText();
        int len = text != null ? text.length() : 0;
        String preview = "";
        if (text != null && !text.isBlank()) {
            preview = text.length() > 600 ? text.substring(0, 600) + "..." : text;
        }

        return new ResumeResponseDto(
                entity.getId(),
                entity.getFilename(),
                entity.getFileType(),
                entity.getFileSizeBytes(),
                entity.getStatus(),
                len,
                preview,
                entity.getErrorMessage(),
                entity.getUploadedAt()
        );
    }
}
