import type {
  AnswerMissingQuestionsRequestDto,
  ApplicationDto,
  ApplicationStatus,
  AuthSessionResponseDto,
  AutomatedDiscoveryResponseDto,
  CancelApplicationRequestDto,
  CandidateProfileDto,
  ConnectorDiagnosticDto,
  CreateJobSourceRequestDto,
  DashboardMetricsDto,
  ImportJobRequestDto,
  JobMatchAnalysisDto,
  JobPlatform,
  JobPostingDto,
  JobSourceDto,
  OpportunityCardDto,
  QaVaultItemDto,
  ResumeAnalyzeResponseDto,
  ResumeDto,
  SearchConfigDto,
  SubmitApplicationRequestDto,
  SyncSourceResponseDto,
  UpdateApplicationStatusRequestDto,
  UpdateProfileRequestDto,
  UpdateSearchConfigRequestDto,
  WorkModality,
} from '../types/api';

const API_BASE = '/api/v1';
const TOKEN_STORAGE_KEY = 'candidauto_jwt_token';

let inMemoryToken: string | null = null;
let pendingLocalSession: Promise<string | null> | null = null;

function readStoredToken(): string | null {
  if (inMemoryToken) return inMemoryToken;
  try {
    const saved = window.sessionStorage?.getItem(TOKEN_STORAGE_KEY);
    if (saved) {
      inMemoryToken = saved;
      return saved;
    }
  } catch {
    // Ambiente sem sessionStorage
  }
  return null;
}

function storeToken(token: string | null): void {
  inMemoryToken = token;
  try {
    if (token) {
      window.sessionStorage?.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      window.sessionStorage?.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch {
    // Ignorar falha de armazenamento em contexto restrito
  }
}

async function ensureAuthToken(): Promise<string | null> {
  const existing = readStoredToken();
  if (existing) return existing;

  if (!pendingLocalSession) {
    pendingLocalSession = (async () => {
      try {
        const res = await fetch(`${API_BASE}/auth/local-session`, {
          method: 'POST',
        });
        if (res.ok) {
          const data = (await res.json()) as AuthSessionResponseDto;
          if (data?.token) {
            storeToken(data.token);
            return data.token;
          }
        }
      } catch {
        // Caso o servidor não esteja em modo local ou em teste unitário mockado
      } finally {
        pendingLocalSession = null;
      }
      return null;
    })();
  }

  return pendingLocalSession;
}

async function fetchWithAuth(
  url: string,
  init: RequestInit = {},
): Promise<Response> {
  const token = await ensureAuthToken();
  const headers = new Headers(init.headers || {});
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(url, {
    ...init,
    headers,
  });

  // Se o backend foi reiniciado com nova chave efêmera e o JWT em sessionStorage expirou/invalidou (401),
  // renova automaticamente a sessão local uma única vez de forma transparente.
  if (response.status === 401 && token && !(init.headers instanceof Headers && init.headers.has('Authorization'))) {
    storeToken(null);
    const freshToken = await ensureAuthToken();
    if (freshToken && freshToken !== token) {
      const retryHeaders = new Headers(init.headers || {});
      retryHeaders.set('Authorization', `Bearer ${freshToken}`);
      return fetch(url, {
        ...init,
        headers: retryHeaders,
      });
    }
  }

  return response;
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = `Erro HTTP ${response.status}`;
    try {
      const errJson = await response.json();
      if (errJson?.message) {
        errorMessage = errJson.message;
      }
      if (Array.isArray(errJson?.details) && errJson.details.length > 0) {
        errorMessage += `: ${errJson.details.join(', ')}`;
      }
    } catch {
      // Fallback caso não seja JSON
    }
    throw new Error(errorMessage);
  }
  return response.json() as Promise<T>;
}

export const jobAssistantApi = {
  // Autenticação Real (JWT Assinado HS256 + BCrypt)
  async registerAccount(payload: {
    name: string;
    email: string;
    password: string;
  }): Promise<AuthSessionResponseDto> {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const session = await parseResponse<AuthSessionResponseDto>(res);
    storeToken(session.token);
    return session;
  },

  async loginAccount(payload: {
    email: string;
    password: string;
  }): Promise<AuthSessionResponseDto> {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const session = await parseResponse<AuthSessionResponseDto>(res);
    storeToken(session.token);
    return session;
  },

  logoutSession(): void {
    storeToken(null);
  },

  async listResumes(): Promise<ResumeDto[]> {
    const res = await fetchWithAuth(`${API_BASE}/resumes`);
    return parseResponse<ResumeDto[]>(res);
  },

  async getResume(id: string): Promise<ResumeDto> {
    const res = await fetchWithAuth(`${API_BASE}/resumes/${id}`);
    return parseResponse<ResumeDto>(res);
  },

  async uploadResume(file: File): Promise<ResumeDto> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetchWithAuth(`${API_BASE}/resumes`, {
      method: 'POST',
      body: formData,
    });
    return parseResponse<ResumeDto>(res);
  },

  async analyzeResume(id: string): Promise<ResumeAnalyzeResponseDto> {
    const res = await fetchWithAuth(`${API_BASE}/resumes/${id}/analyze`, {
      method: 'POST',
    });
    return parseResponse<ResumeAnalyzeResponseDto>(res);
  },

  async getProfile(): Promise<CandidateProfileDto> {
    const res = await fetchWithAuth(`${API_BASE}/profile`);
    return parseResponse<CandidateProfileDto>(res);
  },

  async updateProfile(payload: UpdateProfileRequestDto): Promise<CandidateProfileDto> {
    const res = await fetchWithAuth(`${API_BASE}/profile`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    return parseResponse<CandidateProfileDto>(res);
  },

  async confirmProfile(): Promise<CandidateProfileDto> {
    const res = await fetchWithAuth(`${API_BASE}/profile/confirm`, {
      method: 'POST',
    });
    return parseResponse<CandidateProfileDto>(res);
  },

  // Etapa 2 — Conectores de Fontes e Descoberta de Vagas
  async listJobSources(): Promise<JobSourceDto[]> {
    const res = await fetchWithAuth(`${API_BASE}/job-sources`);
    return parseResponse<JobSourceDto[]>(res);
  },

  async createJobSource(payload: CreateJobSourceRequestDto): Promise<JobSourceDto> {
    const res = await fetchWithAuth(`${API_BASE}/job-sources`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<JobSourceDto>(res);
  },

  async deleteJobSource(id: string): Promise<void> {
    const res = await fetchWithAuth(`${API_BASE}/job-sources/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) {
      throw new Error(`Falha ao remover fonte (HTTP ${res.status})`);
    }
  },

  async syncJobSource(id: string): Promise<SyncSourceResponseDto> {
    const res = await fetchWithAuth(`${API_BASE}/job-sources/${id}/sync`, {
      method: 'POST',
    });
    return parseResponse<SyncSourceResponseDto>(res);
  },

  async discoverJobsAutomatically(): Promise<AutomatedDiscoveryResponseDto> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/discover`, {
      method: 'POST',
    });
    return parseResponse<AutomatedDiscoveryResponseDto>(res);
  },

  async getLatestDiagnostics(): Promise<ConnectorDiagnosticDto[]> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/diagnostics`);
    return parseResponse<ConnectorDiagnosticDto[]>(res);
  },

  async importJob(payload: ImportJobRequestDto): Promise<JobPostingDto> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/import`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<JobPostingDto>(res);
  },

  async listJobs(params?: {
    platform?: JobPlatform;
    modality?: WorkModality;
    activeOnly?: boolean;
    search?: string;
  }): Promise<JobPostingDto[]> {
    const qs = new URLSearchParams();
    if (params?.platform) qs.set('platform', params.platform);
    if (params?.modality) qs.set('modality', params.modality);
    if (params?.activeOnly !== undefined) qs.set('activeOnly', String(params.activeOnly));
    if (params?.search) qs.set('search', params.search);
    const suffix = qs.toString() ? `?${qs.toString()}` : '';
    const res = await fetchWithAuth(`${API_BASE}/jobs${suffix}`);
    return parseResponse<JobPostingDto[]>(res);
  },

  async updateJobAvailability(id: string, active: boolean): Promise<JobPostingDto> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/${id}/availability`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ active }),
    });
    return parseResponse<JobPostingDto>(res);
  },

  // Etapa 3 — Motor de Compatibilidade e Painel de Oportunidades
  async analyzeSingleJobMatch(jobId: string): Promise<JobMatchAnalysisDto> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/${jobId}/match`, {
      method: 'POST',
    });
    return parseResponse<JobMatchAnalysisDto>(res);
  },

  async analyzeAllJobsMatch(): Promise<JobMatchAnalysisDto[]> {
    const res = await fetchWithAuth(`${API_BASE}/jobs/match-all`, {
      method: 'POST',
    });
    return parseResponse<JobMatchAnalysisDto[]>(res);
  },

  async listOpportunities(filters?: {
    minScore?: number;
    area?: string;
    seniority?: string;
    company?: string;
    salary?: string;
    modality?: WorkModality | '';
    location?: string;
    platform?: JobPlatform | '';
    status?: string;
  }): Promise<OpportunityCardDto[]> {
    const qs = new URLSearchParams();
    if (filters?.minScore !== undefined && filters.minScore > 0) qs.set('minScore', String(filters.minScore));
    if (filters?.area) qs.set('area', filters.area);
    if (filters?.seniority) qs.set('seniority', filters.seniority);
    if (filters?.company) qs.set('company', filters.company);
    if (filters?.salary) qs.set('salary', filters.salary);
    if (filters?.modality) qs.set('modality', filters.modality);
    if (filters?.location) qs.set('location', filters.location);
    if (filters?.platform) qs.set('platform', filters.platform);
    if (filters?.status) qs.set('status', filters.status);
    const suffix = qs.toString() ? `?${qs.toString()}` : '';
    const res = await fetchWithAuth(`${API_BASE}/opportunities${suffix}`);
    return parseResponse<OpportunityCardDto[]>(res);
  },

  // Etapas 4, 5 e 6 — Preenchimento Inteligente, Revisão, Candidatura e Acompanhamento
  async prepareApplication(jobId: string): Promise<ApplicationDto> {
    const res = await fetchWithAuth(`${API_BASE}/applications/prepare/${jobId}`, {
      method: 'POST',
    });
    return parseResponse<ApplicationDto>(res);
  },

  async answerMissingQuestions(
    applicationId: string,
    payload: AnswerMissingQuestionsRequestDto,
  ): Promise<ApplicationDto> {
    const res = await fetchWithAuth(`${API_BASE}/applications/${applicationId}/answers`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<ApplicationDto>(res);
  },

  async submitApplication(
    applicationId: string,
    payload: SubmitApplicationRequestDto,
  ): Promise<ApplicationDto> {
    const res = await fetchWithAuth(`${API_BASE}/applications/${applicationId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<ApplicationDto>(res);
  },

  async cancelApplication(
    applicationId: string,
    payload: CancelApplicationRequestDto,
  ): Promise<ApplicationDto> {
    const res = await fetchWithAuth(`${API_BASE}/applications/${applicationId}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<ApplicationDto>(res);
  },

  async updateApplicationStatus(
    applicationId: string,
    payload: UpdateApplicationStatusRequestDto,
  ): Promise<ApplicationDto> {
    const res = await fetchWithAuth(`${API_BASE}/applications/${applicationId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<ApplicationDto>(res);
  },

  async listApplications(status?: ApplicationStatus | ''): Promise<ApplicationDto[]> {
    const suffix = status ? `?status=${encodeURIComponent(status)}` : '';
    const res = await fetchWithAuth(`${API_BASE}/applications${suffix}`);
    return parseResponse<ApplicationDto[]>(res);
  },

  async listQaVault(): Promise<QaVaultItemDto[]> {
    const res = await fetchWithAuth(`${API_BASE}/qa-vault`);
    return parseResponse<QaVaultItemDto[]>(res);
  },

  // Etapa 6 — Automação, Monitoramento e Indicadores do Dashboard
  async getSearchConfig(): Promise<SearchConfigDto> {
    const res = await fetchWithAuth(`${API_BASE}/search-config`);
    return parseResponse<SearchConfigDto>(res);
  },

  async updateSearchConfig(payload: UpdateSearchConfigRequestDto): Promise<SearchConfigDto> {
    const res = await fetchWithAuth(`${API_BASE}/search-config`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return parseResponse<SearchConfigDto>(res);
  },

  async runMonitoringNow(): Promise<SearchConfigDto> {
    const res = await fetchWithAuth(`${API_BASE}/search-config/run-now`, {
      method: 'POST',
    });
    return parseResponse<SearchConfigDto>(res);
  },

  async getDashboardMetrics(): Promise<DashboardMetricsDto> {
    const res = await fetchWithAuth(`${API_BASE}/dashboard/metrics`);
    return parseResponse<DashboardMetricsDto>(res);
  },
};
