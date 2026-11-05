import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from './App';
import { jobAssistantApi } from '../services/api';
import type {
  ApplicationDto,
  CandidateProfileDto,
  DashboardMetricsDto,
  JobPostingDto,
  OpportunityCardDto,
  ResumeDto,
  SearchConfigDto,
} from '../types/api';

vi.mock('../services/api', () => ({
  jobAssistantApi: {
    listResumes: vi.fn(),
    getResume: vi.fn(),
    uploadResume: vi.fn(),
    analyzeResume: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    confirmProfile: vi.fn(),
    listJobSources: vi.fn(),
    createJobSource: vi.fn(),
    deleteJobSource: vi.fn(),
    syncJobSource: vi.fn(),
    importJob: vi.fn(),
    listJobs: vi.fn(),
    updateJobAvailability: vi.fn(),
    analyzeSingleJobMatch: vi.fn(),
    analyzeAllJobsMatch: vi.fn(),
    listOpportunities: vi.fn(),
    prepareApplication: vi.fn(),
    answerMissingQuestions: vi.fn(),
    submitApplication: vi.fn(),
    cancelApplication: vi.fn(),
    updateApplicationStatus: vi.fn(),
    listApplications: vi.fn(),
    listQaVault: vi.fn(),
    getSearchConfig: vi.fn(),
    updateSearchConfig: vi.fn(),
    runMonitoringNow: vi.fn(),
    getDashboardMetrics: vi.fn(),
  },
}));

const mockResume: ResumeDto = {
  id: '11111111-1111-1111-1111-111111111111',
  filename: 'curriculo_rafael.pdf',
  fileType: 'PDF',
  fileSizeBytes: 24576,
  status: 'EXTRACTED',
  extractedTextLength: 420,
  extractedTextPreview: 'Rafael Almeida - Desenvolvedor Full Stack Júnior',
  errorMessage: null,
  uploadedAt: '2026-11-05T15:00:00Z',
};

const mockProfile: CandidateProfileDto = {
  id: '22222222-2222-2222-2222-222222222222',
  userId: '00000000-0000-0000-0000-000000000001',
  resumeId: mockResume.id,
  fullName: 'Rafael Almeida',
  headline: 'Desenvolvedor Full Stack Júnior',
  seniorityLevel: 'Júnior',
  summary: 'Experiência com Java, Spring Boot e React.',
  contactInfo: {
    email: 'rafael@email.com',
    phone: '(11) 99999-0000',
    location: 'São Paulo, SP',
    linkedinUrl: 'linkedin.com/in/rafael',
    githubUrl: 'github.com/rafael',
    portfolioUrl: '',
  },
  experiences: [],
  education: [],
  skills: [
    {
      name: 'Java',
      category: 'LANGUAGE_TECH',
      level: 'Intermediário',
      evidenceExcerpt: 'Experiência com Java, Spring Boot e React.',
    },
  ],
  languages: [],
  certifications: [],
  projects: [],
  targetRoles: ['Desenvolvedor Full Stack Júnior'],
  evidences: [
    {
      id: '33333333-3333-3333-3333-333333333333',
      fieldPath: 'fullName',
      fieldLabel: 'Nome Completo',
      extractedValue: 'Rafael Almeida',
      evidenceText: 'Rafael Almeida',
      verified: true,
      needsReview: false,
    },
    {
      id: '44444444-4444-4444-4444-444444444444',
      fieldPath: 'education',
      fieldLabel: 'Formação Acadêmica',
      extractedValue: '',
      evidenceText:
        'Nenhuma formação acadêmica pôde ser confirmada automaticamente.',
      verified: false,
      needsReview: true,
    },
  ],
  confirmed: false,
  confirmedAt: null,
  updatedAt: '2026-11-05T15:01:00Z',
};

const mockJob: JobPostingDto = {
  id: '55555555-5555-5555-5555-555555555555',
  sourceId: null,
  externalId: '9001',
  sourcePlatform: 'GREENHOUSE',
  companyName: 'Nubank',
  title: 'Software Engineer Júnior',
  descriptionRaw: 'Desenvolvimento em Java, Spring Boot e PostgreSQL.',
  mandatoryRequirements: ['Experiência com Java e Spring Boot'],
  desirableRequirements: ['Conhecimento de AWS'],
  technologies: ['Java', 'Spring Boot', 'PostgreSQL'],
  seniority: 'Júnior',
  workModality: 'HIBRIDO',
  location: 'São Paulo, SP',
  salaryRange: 'R$ 6.500',
  benefits: ['Vale Refeição', 'Plano de Saúde'],
  applicationQuestions: [],
  originalUrl: 'https://boards.greenhouse.io/nubank/jobs/9001',
  publishedAt: '2026-11-05T14:00:00Z',
  discoveredAt: '2026-11-05T15:00:00Z',
  active: true,
};

const mockOpportunity: OpportunityCardDto = {
  job: mockJob,
  matchAnalysis: {
    id: '66666666-6666-6666-6666-666666666666',
    jobId: mockJob.id,
    compatibilityScore: 89,
    scoreBreakdown: {
      technologiesAndRequirementsScore: 45,
      seniorityAndRoleScore: 20,
      modalityAndLocationScore: 15,
      educationAndLanguagesScore: 9,
      desirableRequirementsBonus: 0,
    },
    positivePoints: ['Experiência comprovada com Java e Spring Boot.'],
    attentionPoints: ['Conhecimento de AWS é desejável.'],
    matchedRequirements: ['Tecnologia atendida: Java'],
    missingRequirements: [],
    aiSummary: 'Alta compatibilidade (89%) para Software Engineer Júnior na Nubank.',
    analyzedAt: '2026-11-05T15:05:00Z',
  },
  applicationId: null,
  applicationStatus: 'EM_ANALISE_IA',
};

const mockApplication: ApplicationDto = {
  id: '88888888-8888-8888-8888-888888888888',
  job: mockJob,
  matchAnalysis: mockOpportunity.matchAnalysis,
  status: 'PRONTA_PARA_CANDIDATURA',
  submissionMode: 'INTEGRACAO_AUTORIZADA',
  preparedFields: [
    {
      fieldName: 'first_name_last_name',
      fieldLabel: 'Nome Completo',
      value: 'Rafael Almeida',
      sourceEvidence: 'Extraído e confirmado no Perfil Profissional',
      required: true,
      missing: false,
    },
  ],
  missingQuestions: [],
  userApprovedExplicitly: false,
  submittedAt: null,
  submissionReceipt: null,
  notes: null,
  auditLog: [
    {
      timestamp: '2026-11-05T15:06:00Z',
      status: 'PRONTA_PARA_CANDIDATURA',
      action: 'PREPARACAO_IA',
      details: 'Todos os campos foram preenchidos com dados confirmados.',
    },
  ],
  createdAt: '2026-11-05T15:06:00Z',
  updatedAt: '2026-11-05T15:06:00Z',
};

const mockMetrics: DashboardMetricsDto = {
  totalJobsFound: 1,
  jobsAnalyzedByAi: 1,
  highCompatibilityJobs: 1,
  applicationsPrepared: 1,
  applicationsSubmitted: 1,
  interviewsScheduled: 0,
  conversionRatePercent: 100.0,
};

const mockSearchConfig: SearchConfigDto = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  desiredRoles: ['Desenvolvedor Full Stack Júnior'],
  desiredModalities: ['REMOTO', 'HIBRIDO'],
  desiredSeniorities: ['Júnior'],
  minCompatibilityScore: 70,
  autoMonitoringEnabled: true,
  intervalMinutes: 60,
  lastRunAt: '2026-11-05T15:10:00Z',
  lastRunSummary: 'Ciclo concluído: 1 vaga analisada com alta compatibilidade.',
  updatedAt: '2026-11-05T15:10:00Z',
};

describe('Job Assistant Frontend — Plataforma Completa (Etapas 1 a 6)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(jobAssistantApi.listResumes).mockResolvedValue([mockResume]);
    vi.mocked(jobAssistantApi.getProfile).mockResolvedValue(mockProfile);
    vi.mocked(jobAssistantApi.listJobSources).mockResolvedValue([]);
    vi.mocked(jobAssistantApi.listJobs).mockResolvedValue([mockJob]);
    vi.mocked(jobAssistantApi.listOpportunities).mockResolvedValue([
      mockOpportunity,
    ]);
    vi.mocked(jobAssistantApi.listApplications).mockResolvedValue([
      mockApplication,
    ]);
    vi.mocked(jobAssistantApi.listQaVault).mockResolvedValue([]);
    vi.mocked(jobAssistantApi.getSearchConfig).mockResolvedValue(
      mockSearchConfig
    );
    vi.mocked(jobAssistantApi.getDashboardMetrics).mockResolvedValue(
      mockMetrics
    );
  });

  it('deve exibir o estado do processamento do currículo e permitir upload de PDF/DOCX', async () => {
    const newResume: ResumeDto = {
      ...mockResume,
      id: '99999999-9999-9999-9999-999999999999',
      filename: 'novo_curriculo.docx',
      fileType: 'DOCX',
      status: 'EXTRACTED',
    };
    vi.mocked(jobAssistantApi.uploadResume).mockResolvedValue(newResume);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('status-badge-EXTRACTED')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('nav-resume'));

    const uploadInput = screen.getByTestId('resume-upload-input');
    const file = new File(['conteudo docx'], 'novo_curriculo.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    });

    fireEvent.change(uploadInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(jobAssistantApi.uploadResume).toHaveBeenCalledWith(file);
      expect(
        screen.getAllByText('novo_curriculo.docx').length
      ).toBeGreaterThan(0);
    });
  });

  it('deve apresentar evidências verificadas e destacar campos que precisam de revisão', async () => {
    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.getProfile).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTestId('nav-analysis'));

    expect(screen.getByTestId('evidence-item-fullName')).toBeDefined();
    expect(screen.getByTestId('evidence-item-education')).toBeDefined();
    expect(screen.getByText('Evidência Verificada no Currículo')).toBeDefined();
    expect(screen.getByText('Requer Revisão Humana')).toBeDefined();
  });

  it('deve permitir editar e salvar o perfil profissional manualmente', async () => {
    const updatedProfile: CandidateProfileDto = {
      ...mockProfile,
      fullName: 'Rafael Almeida Atualizado',
    };
    vi.mocked(jobAssistantApi.updateProfile).mockResolvedValue(updatedProfile);

    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.getProfile).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTestId('nav-profile'));

    const nameInput = screen.getByTestId('profile-fullname-input');
    fireEvent.change(nameInput, {
      target: { value: 'Rafael Almeida Atualizado' },
    });

    fireEvent.click(screen.getByTestId('save-profile-btn'));

    await waitFor(() => {
      expect(jobAssistantApi.updateProfile).toHaveBeenCalled();
      expect(
        screen.getByText(/Perfil profissional atualizado com sucesso/i)
      ).toBeDefined();
    });
  });

  it('deve executar o fluxo de confirmação explícita do perfil', async () => {
    const confirmedProfile: CandidateProfileDto = {
      ...mockProfile,
      confirmed: true,
      confirmedAt: '2026-11-05T15:10:00Z',
    };
    vi.mocked(jobAssistantApi.confirmProfile).mockResolvedValue(
      confirmedProfile
    );

    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.getProfile).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTestId('nav-validation'));

    const confirmBtn = screen.getByTestId('confirm-profile-btn');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(jobAssistantApi.confirmProfile).toHaveBeenCalled();
      expect(screen.getByTestId('profile-confirmed-banner')).toBeDefined();
    });
  });

  it('deve listar vagas normalizadas e permitir importar nova oportunidade na Etapa 2', async () => {
    const importedJob: JobPostingDto = {
      ...mockJob,
      id: '77777777-7777-7777-7777-777777777777',
      sourcePlatform: 'LINKEDIN',
      companyName: 'Mercado Livre',
      title: 'Desenvolvedor Back-end Júnior',
    };
    vi.mocked(jobAssistantApi.importJob).mockResolvedValue(importedJob);

    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.listJobs).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTestId('nav-jobs'));

    expect(screen.getByTestId(`job-card-${mockJob.id}`)).toBeDefined();
    expect(screen.getByText('Software Engineer Júnior')).toBeDefined();

    fireEvent.click(screen.getByTestId('toggle-import-job-btn'));

    fireEvent.change(screen.getByTestId('import-company-input'), {
      target: { value: 'Mercado Livre' },
    });
    fireEvent.change(screen.getByTestId('import-title-input'), {
      target: { value: 'Desenvolvedor Back-end Júnior' },
    });
    fireEvent.change(screen.getByTestId('import-url-input'), {
      target: { value: 'https://linkedin.com/jobs/view/123' },
    });
    fireEvent.change(screen.getByTestId('import-description-input'), {
      target: { value: 'Experiência com Java, Spring Boot e PostgreSQL.' },
    });

    fireEvent.click(screen.getByTestId('submit-import-job-btn'));

    await waitFor(() => {
      expect(jobAssistantApi.importJob).toHaveBeenCalled();
      expect(screen.getByText('Desenvolvedor Back-end Júnior')).toBeDefined();
    });
  });

  it('deve exibir o Painel de Oportunidades com score 89%, pontos positivos/atenção e permitir aprovar candidatura com BOTÃO CANDIDATAR-SE (Etapas 3, 4 e 5)', async () => {
    vi.mocked(jobAssistantApi.submitApplication).mockResolvedValue({
      ...mockApplication,
      status: 'CANDIDATURA_ENVIADA',
      userApprovedExplicitly: true,
      submittedAt: '2026-11-05T15:20:00Z',
      submissionReceipt: 'Candidatura aprovada explicitamente',
    });

    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.listOpportunities).toHaveBeenCalled();
    });

    // Navegar para Oportunidades (Etapa 3)
    fireEvent.click(screen.getByTestId('nav-opportunities'));

    expect(screen.getByText('89%')).toBeDefined();
    expect(
      screen.getByText('Experiência comprovada com Java e Spring Boot.')
    ).toBeDefined();
    expect(screen.getByText('Conhecimento de AWS é desejável.')).toBeDefined();

    // Navegar para Central de Candidaturas (Etapas 4, 5 e 6)
    fireEvent.click(screen.getByTestId('nav-applications'));

    const submitBtn = screen.getByTestId('btn-candidatar-se');
    expect(submitBtn).toBeDefined();
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(jobAssistantApi.submitApplication).toHaveBeenCalledWith(
        mockApplication.id,
        expect.objectContaining({ explicitApprovalConfirmed: true })
      );
    });
  });

  it('deve exibir e executar o monitoramento automático na aba Automação (Etapa 6)', async () => {
    vi.mocked(jobAssistantApi.runMonitoringNow).mockResolvedValue(
      mockSearchConfig
    );

    render(<App />);

    await waitFor(() => {
      expect(jobAssistantApi.getSearchConfig).toHaveBeenCalled();
    });

    fireEvent.click(screen.getByTestId('nav-automation'));

    expect(
      screen.getByText('Configurações de Pesquisa e Automação')
    ).toBeDefined();

    fireEvent.click(screen.getByTestId('btn-run-monitoring-now'));

    await waitFor(() => {
      expect(jobAssistantApi.runMonitoringNow).toHaveBeenCalled();
    });
  });

  it('deve tratar e apresentar erros da API com segurança na interface', async () => {
    vi.mocked(jobAssistantApi.listResumes).mockRejectedValueOnce(
      new Error('Falha de comunicação com o servidor da API.')
    );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByTestId('global-error-banner')).toBeDefined();
      expect(
        screen.getByText('Falha de comunicação com o servidor da API.')
      ).toBeDefined();
    });
  });
});
