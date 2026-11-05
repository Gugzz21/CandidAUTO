package com.jobassistant.profile.domain;

import com.jobassistant.profile.dto.CertificationItemDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.ExperienceItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.ProjectItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfileEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(length = 255)
    private String headline;

    @Column(name = "seniority_level", length = 60)
    private String seniorityLevel;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact_info", nullable = false, columnDefinition = "jsonb")
    private ContactInfoDto contactInfo = ContactInfoDto.empty();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ExperienceItemDto> experiences = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<EducationItemDto> education = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<SkillItemDto> skills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<LanguageItemDto> languages = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<CertificationItemDto> certifications = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ProjectItemDto> projects = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_roles", nullable = false, columnDefinition = "jsonb")
    private List<String> targetRoles = new ArrayList<>();

    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CandidateProfileEntity() {
    }

    public CandidateProfileEntity(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSeniorityLevel() {
        return seniorityLevel;
    }

    public void setSeniorityLevel(String seniorityLevel) {
        this.seniorityLevel = seniorityLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ContactInfoDto getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfoDto contactInfo) {
        this.contactInfo = contactInfo != null ? contactInfo : ContactInfoDto.empty();
    }

    public List<ExperienceItemDto> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceItemDto> experiences) {
        this.experiences = experiences != null ? experiences : new ArrayList<>();
    }

    public List<EducationItemDto> getEducation() {
        return education;
    }

    public void setEducation(List<EducationItemDto> education) {
        this.education = education != null ? education : new ArrayList<>();
    }

    public List<SkillItemDto> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillItemDto> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    public List<LanguageItemDto> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageItemDto> languages) {
        this.languages = languages != null ? languages : new ArrayList<>();
    }

    public List<CertificationItemDto> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<CertificationItemDto> certifications) {
        this.certifications = certifications != null ? certifications : new ArrayList<>();
    }

    public List<ProjectItemDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectItemDto> projects) {
        this.projects = projects != null ? projects : new ArrayList<>();
    }

    public List<String> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(List<String> targetRoles) {
        this.targetRoles = targetRoles != null ? targetRoles : new ArrayList<>();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
