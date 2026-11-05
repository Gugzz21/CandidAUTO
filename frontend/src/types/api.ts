export type ResumeStatus =
  | 'UPLOADED'
  | 'EXTRACTING'
  | 'EXTRACTED'
  | 'ANALYZING'
  | 'REVIEW_REQUIRED'
  | 'CONFIRMED'
  | 'FAILED';

export type JobPlatform =
  | 'GREENHOUSE'
  | 'LEVER'
  | 'GUPY'
  | 'CAREER_PAGE'
  | 'LINKEDIN'
  | 'INDEED'
  | 'GLASSDOOR'
  | 'PROGRAMATHOR'
  | 'REVELO'
  | 'INFOJOBS'
  | 'MANUAL_URL';

export type WorkModality = 'REMOTO' | 'HIBRIDO' | 'PRESENCIAL' | 'NAO_INFORMADO';

export type ApplicationStatus =
  | 'ENCONTRADA'
  | 'EM_ANALISE_IA'
  | 'AGUARDANDO_REVISAO'
  | 'PRONTA_PARA_CANDIDATURA'
  | 'CANDIDATURA_ENVIADA'
  | 'EM_ANDAMENTO'
  | 'ENTREVISTA_AGENDADA'
  | 'APROVADO'
  | 'REPROVADO'
  | 'CANCELADA'
  | 'VAGA_ENCERRADA';

export type SubmissionMode = 'INTEGRACAO_AUTORIZADA' | 'FLUXO_ASSISTIDO';

export interface ResumeDto {
  id: string;
  filename: string;
  fileType: 'PDF' | 'DOCX';
  fileSizeBytes: number;
  status: ResumeStatus;
  extractedTextLength: number;
  extractedTextPreview: string;
  errorMessage: string | null;
  uploadedAt: string;
}

export interface ContactInfoDto {
  email: string;
  phone: string;
  location: string;
  linkedinUrl: string;
  githubUrl: string;
  portfolioUrl: string;
}

export interface ExperienceItemDto {
  company: string;
  role: string;
  startDate: string;
  endDate: string;
  current: boolean;
  description: string;
  technologies: string[];
  evidenceExcerpt: string;
}

export interface EducationItemDto {
  institution: string;
  degree: string;
  fieldOfStudy: string;
  startDate: string;
  endDate: string;
  status: string;
  evidenceExcerpt: string;
}

export interface SkillItemDto {
  name: string;
  category: string;
  level: string;
  evidenceExcerpt: string;
}

export interface LanguageItemDto {
  language: string;
  proficiency: string;
  evidenceExcerpt: string;
}

export interface CertificationItemDto {
  name: string;
  issuer: string;
  year: string;
  evidenceExcerpt: string;
}

export interface ProjectItemDto {
  name: string;
  description: string;
  technologies: string[];
  url: string;
  evidenceExcerpt: string;
}

export interface ProfileEvidenceDto {
  id: string;
  fieldPath: string;
  fieldLabel: string;
  extractedValue: string;
  evidenceText: string;
  verified: boolean;
  needsReview: boolean;
}

export interface CandidateProfileDto {
  id: string | null;
  userId: string;
  resumeId: string | null;
  fullName: string;
  headline: string;
  seniorityLevel: string;
  summary: string;
  contactInfo: ContactInfoDto;
  experiences: ExperienceItemDto[];
  education: EducationItemDto[];
  skills: SkillItemDto[];
  languages: LanguageItemDto[];
  certifications: CertificationItemDto[];
  projects: ProjectItemDto[];
  targetRoles: string[];
  evidences: ProfileEvidenceDto[];
  confirmed: boolean;
  confirmedAt: string | null;
  updatedAt: string | null;
}

export interface ResumeAnalyzeResponseDto {
  resume: ResumeDto;
  profile: CandidateProfileDto;
}

export interface UpdateProfileRequestDto {
  fullName: string;
  headline: string;
  seniorityLevel: string;
  summary: string;
  contactInfo: ContactInfoDto;
  experiences: ExperienceItemDto[];
  education: EducationItemDto[];
  skills: SkillItemDto[];
  languages: LanguageItemDto[];
  certifications: CertificationItemDto[];
  projects: ProjectItemDto[];
  targetRoles: string[];
}

export interface ApplicationQuestionDto {
  name: string;
  label: string;
  type: string;
  required: boolean;
  options: string[];
}

export interface JobSourceDto {
  id: string;
  name: string;
  platform: JobPlatform;
  boardIdentifier: string;
  enabled: boolean;
  lastSyncedAt: string | null;
  lastSyncStatus: string | null;
  createdAt: string;
}

export interface CreateJobSourceRequestDto {
  name: string;
  platform: JobPlatform;
  boardIdentifier: string;
}

export interface JobPostingDto {
  id: string;
  sourceId: string | null;
  externalId: string | null;
  sourcePlatform: JobPlatform;
  companyName: string;
  title: string;
  descriptionRaw: string;
  mandatoryRequirements: string[];
  desirableRequirements: string[];
  technologies: string[];
  seniority: string;
  workModality: WorkModality;
  location: string;
  salaryRange: string;
  benefits: string[];
  applicationQuestions: ApplicationQuestionDto[];
  originalUrl: string;
  publishedAt: string | null;
  discoveredAt: string;
  active: boolean;
}

export interface ImportJobRequestDto {
  sourcePlatform: JobPlatform;
  companyName: string;
  title: string;
  location?: string;
  workModality?: WorkModality;
  seniority?: string;
  salaryRange?: string;
  originalUrl: string;
  descriptionRaw: string;
}

export interface SyncSourceResponseDto {
  source: JobSourceDto;
  totalFetched: number;
  newlyInserted: number;
  updatedExisting: number;
  markedExpired: number;
  jobs: JobPostingDto[];
}

export interface ScoreBreakdownDto {
  technologiesAndRequirementsScore: number;
  seniorityAndRoleScore: number;
  modalityAndLocationScore: number;
  educationAndLanguagesScore: number;
  desirableRequirementsBonus: number;
}

export interface JobMatchAnalysisDto {
  id: string;
  jobId: string;
  compatibilityScore: number;
  scoreBreakdown: ScoreBreakdownDto;
  positivePoints: string[];
  attentionPoints: string[];
  matchedRequirements: string[];
  missingRequirements: string[];
  aiSummary: string;
  analyzedAt: string;
}

export interface OpportunityCardDto {
  job: JobPostingDto;
  matchAnalysis: JobMatchAnalysisDto | null;
  applicationId: string | null;
  applicationStatus: ApplicationStatus;
}

export interface PreparedFieldDto {
  fieldName: string;
  fieldLabel: string;
  value: string;
  sourceEvidence: string;
  required: boolean;
  missing: boolean;
}

export interface MissingQuestionDto {
  questionKey: string;
  questionLabel: string;
  required: boolean;
  reason: string;
}

export interface ApplicationAuditEventDto {
  timestamp: string;
  status: string;
  action: string;
  details: string;
}

export interface ApplicationDto {
  id: string;
  job: JobPostingDto;
  matchAnalysis: JobMatchAnalysisDto | null;
  status: ApplicationStatus;
  submissionMode: SubmissionMode;
  preparedFields: PreparedFieldDto[];
  missingQuestions: MissingQuestionDto[];
  userApprovedExplicitly: boolean;
  submittedAt: string | null;
  submissionReceipt: string | null;
  notes: string | null;
  auditLog: ApplicationAuditEventDto[];
  createdAt: string;
  updatedAt: string;
}

export interface AnswerMissingQuestionsRequestDto {
  answers: Record<string, string>;
  saveToVault: boolean;
}

export interface SubmitApplicationRequestDto {
  explicitApprovalConfirmed: boolean;
  notes?: string;
}

export interface CancelApplicationRequestDto {
  keepAsDraft: boolean;
  reason?: string;
}

export interface UpdateApplicationStatusRequestDto {
  status: ApplicationStatus;
  notes?: string;
}

export interface QaVaultItemDto {
  id: string;
  questionKey: string;
  questionLabel: string;
  confirmedAnswer: string;
  updatedAt: string;
}

export interface SearchConfigDto {
  id: string;
  desiredRoles: string[];
  desiredModalities: string[];
  desiredSeniorities: string[];
  minCompatibilityScore: number;
  autoMonitoringEnabled: boolean;
  intervalMinutes: number;
  lastRunAt: string | null;
  lastRunSummary: string | null;
  updatedAt: string;
}

export interface UpdateSearchConfigRequestDto {
  desiredRoles: string[];
  desiredModalities: string[];
  desiredSeniorities: string[];
  minCompatibilityScore: number;
  autoMonitoringEnabled: boolean;
  intervalMinutes: number;
}

export interface DashboardMetricsDto {
  totalJobsFound: number;
  jobsAnalyzedByAi: number;
  highCompatibilityJobs: number;
  applicationsPrepared: number;
  applicationsSubmitted: number;
  interviewsScheduled: number;
  conversionRatePercent: number;
}
