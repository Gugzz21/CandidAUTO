package com.jobassistant.profile.controller;

import com.jobassistant.profile.dto.CandidateProfileDto;
import com.jobassistant.profile.dto.UpdateProfileRequestDto;
import com.jobassistant.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<CandidateProfileDto> getProfile() {
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @PutMapping
    public ResponseEntity<CandidateProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(profileService.updateCurrentUserProfile(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<CandidateProfileDto> confirmProfile() {
        return ResponseEntity.ok(profileService.confirmCurrentUserProfile());
    }
}
