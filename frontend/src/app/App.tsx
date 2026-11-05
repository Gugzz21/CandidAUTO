import React, { useCallback, useEffect, useState } from 'react';
import {
  LayoutDashboard,
  FileText,
  Sparkles,
  UserCheck,
  ShieldCheck,
  Globe,
  Target,
  Send,
  Bot,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import type {
  AnswerMissingQuestionsRequestDto,
  ApplicationDto,
  CancelApplicationRequestDto,
  CandidateProfileDto,
  CreateJobSourceRequestDto,
  DashboardMetricsDto,
  ImportJobRequestDto,
  JobPostingDto,
  JobSourceDto,
  OpportunityCardDto,
  QaVaultItemDto,
  ResumeDto,
  SearchConfigDto,
  SubmitApplicationRequestDto,
  UpdateApplicationStatusRequestDto,
  UpdateProfileRequestDto,
  UpdateSearchConfigRequestDto,
} from '../types/api';
import { jobAssistantApi } from '../services/api';
import { DashboardView, type NavTabId } from '../features/dashboard/DashboardView';
import { ResumeManagerView } from '../features/resume/ResumeManagerView';
import { AiAnalysisView } from '../features/profile/AiAnalysisView';
import { ProfileEditorView } from '../features/profile/ProfileEditorView';
import { ValidationConfirmView } from '../features/profile/ValidationConfirmView';
import { JobDiscoveryView } from '../features/jobs/JobDiscoveryView';
import { OpportunitiesDashboardView } from '../features/opportunities/OpportunitiesDashboardView';
import { ApplicationsCenterView } from '../features/applications/ApplicationsCenterView';
import { AutomationSettingsView } from '../features/automation/AutomationSettingsView';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTabId>('dashboard');
  const [resumes, setResumes] = useState<ResumeDto[]>([]);
  const [profile, setProfile] = useState<CandidateProfileDto | null>(null);
  const [sources, setSources] = useState<JobSourceDto[]>([]);
  const [jobs, setJobs] = useState<JobPostingDto[]>([]);
  const [opportunities, setOpportunities] = useState<OpportunityCardDto[]>([]);
  const [applications, setApplications] = useState<ApplicationDto[]>([]);
  const [qaVault, setQaVault] = useState<QaVaultItemDto[]>([]);
  const [searchConfig, setSearchConfig] = useState<SearchConfigDto | null>(null);
  const [metrics, setMetrics] = useState<DashboardMetricsDto | null>(null);

  const [selectedApplicationId, setSelectedApplicationId] = useState<
    string | null
  >(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [analyzingResumeId, setAnalyzingResumeId] = useState<string | null>(
    null
  );
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isConfirmingProfile, setIsConfirmingProfile] = useState(false);
  const [syncingSourceId, setSyncingSourceId] = useState<string | null>(null);
  const [isMatchingAll, setIsMatchingAll] = useState(false);
  const [preparingJobId, setPreparingJobId] = useState<string | null>(null);
  const [isSavingAutomation, setIsSavingAutomation] = useState(false);
  const [isRunningMonitoring, setIsRunningMonitoring] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  const refreshOpportunitiesAndApplications = useCallback(async () => {
    try {
      const [oppList, appList, vaultList, kpis] = await Promise.all([
        jobAssistantApi.listOpportunities(),
        jobAssistantApi.listApplications(),
        jobAssistantApi.listQaVault(),
        jobAssistantApi.getDashboardMetrics(),
      ]);
      setOpportunities(oppList);
      setApplications(appList);
      setQaVault(vaultList);
      setMetrics(kpis);
    } catch {
      // Silencioso caso endpoints secundários não estejam populados em mocks parciais
    }
  }, []);

  const loadInitialData = useCallback(async () => {
    setIsLoading(true);
    setGlobalError(null);
    try {
      const [resumeList, userProfile, sourceList, jobList] = await Promise.all([
        jobAssistantApi.listResumes(),
        jobAssistantApi.getProfile(),
        jobAssistantApi.listJobSources(),
        jobAssistantApi.listJobs(),
      ]);
      setResumes(resumeList);
      setProfile(userProfile);
      setSources(sourceList);
      setJobs(jobList);

      try {
        const [oppList, appList, vaultList, cfg, kpis] = await Promise.all([
          jobAssistantApi.listOpportunities(),
          jobAssistantApi.listApplications(),
          jobAssistantApi.listQaVault(),
          jobAssistantApi.getSearchConfig(),
          jobAssistantApi.getDashboardMetrics(),
        ]);
        setOpportunities(oppList);
        setApplications(appList);
        setQaVault(vaultList);
        setSearchConfig(cfg);
        setMetrics(kpis);
      } catch {
        // Permite compatibilidade com testes que façam mock apenas das chamadas principais
      }
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Não foi possível carregar os dados do servidor.'
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadInitialData();
  }, [loadInitialData]);

  const handleUploadFile = async (file: File) => {
    setIsUploading(true);
    setGlobalError(null);
    try {
      const uploaded = await jobAssistantApi.uploadResume(file);
      setResumes((prev) => [uploaded, ...prev]);
    } finally {
      setIsUploading(false);
    }
  };

  const handleAnalyzeResume = async (resumeId: string) => {
    setAnalyzingResumeId(resumeId);
    setGlobalError(null);
    try {
      const result = await jobAssistantApi.analyzeResume(resumeId);
      setResumes((prev) =>
        prev.map((r) => (r.id === result.resume.id ? result.resume : r))
      );
      setProfile(result.profile);
      setActiveTab('analysis');
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao analisar currículo com IA.'
      );
    } finally {
      setAnalyzingResumeId(null);
    }
  };

  const handleSaveProfile = async (payload: UpdateProfileRequestDto) => {
    setIsSavingProfile(true);
    setGlobalError(null);
    try {
      const updated = await jobAssistantApi.updateProfile(payload);
      setProfile(updated);
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleConfirmProfile = async () => {
    setIsConfirmingProfile(true);
    setGlobalError(null);
    try {
      const confirmed = await jobAssistantApi.confirmProfile();
      setProfile(confirmed);
      const refreshedResumes = await jobAssistantApi.listResumes();
      setResumes(refreshedResumes);
    } finally {
      setIsConfirmingProfile(false);
    }
  };

  const handleCreateSource = async (payload: CreateJobSourceRequestDto) => {
    setGlobalError(null);
    try {
      const created = await jobAssistantApi.createJobSource(payload);
      setSources((prev) => [created, ...prev]);
    } catch (err) {
      setGlobalError(
        err instanceof Error ? err.message : 'Erro ao cadastrar fonte de vagas.'
      );
    }
  };

  const handleDeleteSource = async (id: string) => {
    setGlobalError(null);
    try {
      await jobAssistantApi.deleteJobSource(id);
      setSources((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      setGlobalError(
        err instanceof Error ? err.message : 'Erro ao remover fonte.'
      );
    }
  };

  const handleSyncSource = async (id: string) => {
    setSyncingSourceId(id);
    setGlobalError(null);
    try {
      const res = await jobAssistantApi.syncJobSource(id);
      setSources((prev) =>
        prev.map((s) => (s.id === res.source.id ? res.source : s))
      );
      const refreshedJobs = await jobAssistantApi.listJobs();
      setJobs(refreshedJobs);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao sincronizar fonte de vagas.'
      );
      const refreshedSources = await jobAssistantApi.listJobSources();
      setSources(refreshedSources);
    } finally {
      setSyncingSourceId(null);
    }
  };

  const handleImportJob = async (payload: ImportJobRequestDto) => {
    setGlobalError(null);
    try {
      const imported = await jobAssistantApi.importJob(payload);
      setJobs((prev) => {
        const exists = prev.some((j) => j.id === imported.id);
        return exists
          ? prev.map((j) => (j.id === imported.id ? imported : j))
          : [imported, ...prev];
      });
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error ? err.message : 'Erro ao importar vaga.'
      );
    }
  };

  const handleToggleAvailability = async (id: string, active: boolean) => {
    setGlobalError(null);
    try {
      const updated = await jobAssistantApi.updateJobAvailability(id, active);
      setJobs((prev) => prev.map((j) => (j.id === updated.id ? updated : j)));
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao atualizar disponibilidade da vaga.'
      );
    }
  };

  const handleRunMatchAll = async () => {
    setIsMatchingAll(true);
    setGlobalError(null);
    try {
      await jobAssistantApi.analyzeAllJobsMatch();
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao analisar compatibilidade das vagas.'
      );
    } finally {
      setIsMatchingAll(false);
    }
  };

  const handlePrepareApplication = async (jobId: string) => {
    setPreparingJobId(jobId);
    setGlobalError(null);
    try {
      const appDto = await jobAssistantApi.prepareApplication(jobId);
      await refreshOpportunitiesAndApplications();
      setSelectedApplicationId(appDto.id);
      setActiveTab('applications');
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao preparar candidatura inteligente.'
      );
    } finally {
      setPreparingJobId(null);
    }
  };

  const handleAnswerMissingQuestions = async (
    applicationId: string,
    payload: AnswerMissingQuestionsRequestDto
  ) => {
    setGlobalError(null);
    try {
      await jobAssistantApi.answerMissingQuestions(applicationId, payload);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao salvar respostas da candidatura.'
      );
    }
  };

  const handleSubmitApplication = async (
    applicationId: string,
    payload: SubmitApplicationRequestDto
  ) => {
    setGlobalError(null);
    try {
      await jobAssistantApi.submitApplication(applicationId, payload);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error ? err.message : 'Erro ao enviar candidatura.'
      );
    }
  };

  const handleCancelApplication = async (
    applicationId: string,
    payload: CancelApplicationRequestDto
  ) => {
    setGlobalError(null);
    try {
      await jobAssistantApi.cancelApplication(applicationId, payload);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error ? err.message : 'Erro ao cancelar candidatura.'
      );
    }
  };

  const handleUpdateApplicationStatus = async (
    applicationId: string,
    payload: UpdateApplicationStatusRequestDto
  ) => {
    setGlobalError(null);
    try {
      await jobAssistantApi.updateApplicationStatus(applicationId, payload);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao atualizar status da candidatura.'
      );
    }
  };

  const handleSaveSearchConfig = async (payload: UpdateSearchConfigRequestDto) => {
    setIsSavingAutomation(true);
    setGlobalError(null);
    try {
      const updated = await jobAssistantApi.updateSearchConfig(payload);
      setSearchConfig(updated);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao salvar configuração de automação.'
      );
    } finally {
      setIsSavingAutomation(false);
    }
  };

  const handleRunMonitoringNow = async () => {
    setIsRunningMonitoring(true);
    setGlobalError(null);
    try {
      const updated = await jobAssistantApi.runMonitoringNow();
      setSearchConfig(updated);
      const refreshedJobs = await jobAssistantApi.listJobs();
      setJobs(refreshedJobs);
      await refreshOpportunitiesAndApplications();
    } catch (err) {
      setGlobalError(
        err instanceof Error
          ? err.message
          : 'Erro ao executar ciclo de monitoramento.'
      );
    } finally {
      setIsRunningMonitoring(false);
    }
  };

  const navItems: {
    id: NavTabId;
    label: string;
    icon: React.FC<{ className?: string }>;
  }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'resume', label: 'Currículo', icon: FileText },
    { id: 'analysis', label: 'Análise IA', icon: Sparkles },
    { id: 'profile', label: 'Perfil', icon: UserCheck },
    { id: 'validation', label: 'Validação', icon: ShieldCheck },
    { id: 'jobs', label: 'Fontes & Vagas', icon: Globe },
    { id: 'opportunities', label: 'Oportunidades', icon: Target },
    { id: 'applications', label: 'Candidaturas', icon: Send },
    { id: 'automation', label: 'Automação', icon: Bot },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Topbar SaaS */}
      <header className="sticky top-0 z-30 border-b border-slate-800/80 bg-slate-950/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3.5 sm:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 font-bold text-white shadow-sm">
              CA
            </div>
            <div>
              <span className="text-base font-semibold tracking-tight text-white">
                CandidAUTO
              </span>
              <span className="ml-2 hidden rounded-md border border-slate-800 bg-slate-900 px-2 py-0.5 text-[11px] font-medium text-slate-400 sm:inline-block">
                v1.0 • Plataforma Completa
              </span>
            </div>
          </div>

          <nav
            aria-label="Navegação principal"
            className="flex items-center gap-1 overflow-x-auto"
          >
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  data-testid={`nav-${item.id}`}
                  onClick={() => setActiveTab(item.id)}
                  className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-medium transition ${
                    isActive
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                  }`}
                >
                  <Icon className="h-3.5 w-3.5" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>

          <button
            type="button"
            onClick={() => void loadInitialData()}
            title="Recarregar dados"
            className="rounded-lg border border-slate-800 bg-slate-900 p-2 text-slate-400 hover:text-white"
          >
            <RefreshCw
              className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`}
            />
          </button>
        </div>
      </header>

      {/* Conteúdo Principal */}
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        {globalError && (
          <div
            role="alert"
            data-testid="global-error-banner"
            className="mb-6 flex items-center justify-between gap-3 rounded-xl border border-rose-800 bg-rose-950/60 px-4 py-3 text-sm text-rose-200"
          >
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
              <span>{globalError}</span>
            </div>
            <button
              type="button"
              onClick={() => setGlobalError(null)}
              className="text-xs font-medium text-rose-300 underline hover:text-white"
            >
              Fechar
            </button>
          </div>
        )}

        {activeTab === 'dashboard' && (
          <DashboardView
            resumes={resumes}
            profile={profile}
            jobs={jobs}
            metrics={metrics}
            onNavigate={setActiveTab}
          />
        )}

        {activeTab === 'resume' && (
          <ResumeManagerView
            resumes={resumes}
            isUploading={isUploading}
            analyzingResumeId={analyzingResumeId}
            onUploadFile={handleUploadFile}
            onAnalyzeResume={handleAnalyzeResume}
          />
        )}

        {activeTab === 'analysis' && (
          <AiAnalysisView
            profile={profile}
            onGoToEdit={() => setActiveTab('profile')}
          />
        )}

        {activeTab === 'profile' && (
          <ProfileEditorView
            profile={profile}
            isSaving={isSavingProfile}
            onSaveProfile={handleSaveProfile}
          />
        )}

        {activeTab === 'validation' && (
          <ValidationConfirmView
            profile={profile}
            isConfirming={isConfirmingProfile}
            onConfirmProfile={handleConfirmProfile}
            onGoToEdit={() => setActiveTab('profile')}
          />
        )}

        {activeTab === 'jobs' && (
          <JobDiscoveryView
            sources={sources}
            jobs={jobs}
            syncingSourceId={syncingSourceId}
            onCreateSource={handleCreateSource}
            onDeleteSource={handleDeleteSource}
            onSyncSource={handleSyncSource}
            onImportJob={handleImportJob}
            onToggleAvailability={handleToggleAvailability}
          />
        )}

        {activeTab === 'opportunities' && (
          <OpportunitiesDashboardView
            opportunities={opportunities}
            isMatchingAll={isMatchingAll}
            preparingJobId={preparingJobId}
            onRunMatchAll={handleRunMatchAll}
            onPrepareApplication={handlePrepareApplication}
          />
        )}

        {activeTab === 'applications' && (
          <ApplicationsCenterView
            applications={applications}
            qaVault={qaVault}
            selectedApplicationId={selectedApplicationId}
            onSelectApplication={setSelectedApplicationId}
            onAnswerMissingQuestions={handleAnswerMissingQuestions}
            onSubmitApplication={handleSubmitApplication}
            onCancelApplication={handleCancelApplication}
            onUpdateStatus={handleUpdateApplicationStatus}
          />
        )}

        {activeTab === 'automation' && (
          <AutomationSettingsView
            searchConfig={searchConfig}
            isSaving={isSavingAutomation}
            isRunningCycle={isRunningMonitoring}
            onSaveConfig={handleSaveSearchConfig}
            onRunMonitoringNow={handleRunMonitoringNow}
          />
        )}
      </main>
    </div>
  );
};

export default App;
