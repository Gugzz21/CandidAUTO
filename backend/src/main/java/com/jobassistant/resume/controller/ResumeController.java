package com.jobassistant.resume.controller;

import com.jobassistant.resume.dto.ResumeAnalyzeResponseDto;
import com.jobassistant.resume.dto.ResumeResponseDto;
import com.jobassistant.resume.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponseDto> uploadResume(@RequestParam("file") MultipartFile file) {
        ResumeResponseDto created = resumeService.uploadAndExtract(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponseDto>> listResumes() {
        return ResponseEntity.ok(resumeService.listCurrentUserResumes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponseDto> getResume(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.getResumeById(id));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ResumeAnalyzeResponseDto> analyzeResume(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.analyzeResume(id));
    }
}
