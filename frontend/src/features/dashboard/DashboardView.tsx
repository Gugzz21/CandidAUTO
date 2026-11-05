import React from 'react';
import {
  CheckCircle2,
  FileText,
  ArrowRight,
  ShieldCheck,
  Briefcase,
  Globe,
  Sparkles,
  Send,
  CalendarCheck,
  TrendingUp,
  Target,
} from 'lucide-react';
import type {
  CandidateProfileDto,
  DashboardMetricsDto,
  JobPostingDto,
  ResumeDto,
} from '../../types/api';
import { StatusBadge } from '../../components/StatusBadge';

export type NavTabId =
  | 'dashboard'
  | 'resume'
  | 'analysis'
  | 'profile'
  | 'validation'
  | 'jobs'
  | 'opportunities'
  | 'applications'
  | 'automation';

interface DashboardViewProps {
  resumes: ResumeDto[];
  profile: CandidateProfileDto | null;
  jobs?: JobPostingDto[];
  metrics?: DashboardMetricsDto | null;
  onNavigate: (tab: NavTabId) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  resumes,
  profile,
  jobs = [],
  metrics,
  onNavigate,
}) => {
  const latestResume = resumes[0] ?? null;
  const verifiedEvidences =
    profile?.evidences.filter((e) => e.verified).length ?? 0;
  const reviewPendingEvidences =
    profile?.evidences.filter((e) => e.needsReview).length ?? 0;
  const activeJobsCount = jobs.filter((j) => j.active).length;

  const totalJobsFound = metrics?.totalJobsFound ?? jobs.length;
  const jobsAnalyzedByAi = metrics?.jobsAnalyzedByAi ?? 0;
  const highCompatibilityJobs = metrics?.highCompatibilityJobs ?? 0;
  const applicationsSubmitted = metrics?.applicationsSubmitted ?? 0;
  const interviewsScheduled = metrics?.interviewsScheduled ?? 0;
  const conversionRatePercent = metrics?.conversionRatePercent ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800/80 bg-gradient-to-br from-slate-900 via-slate-900 to-indigo-950/30 p-6 sm:flex-row sm:items-center">
        <div>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 text-xs font-medium text-indigo-300">
            <ShieldCheck className="h-3.5 w-3.5" />
            Personal First • SaaS Ready (Etapas 1 a 6 Completas)
          </span>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-white">
            Agente Inteligente de Recrutamento e Candidaturas
          </h1>
          <p className="mt-1 max-w-2xl text-sm text-slate-400">
            Analise seu currículo sem alucinações, descubra oportunidades reais, avalie a compatibilidade (0–100%), preencha formulários com dados confirmados e aprove cada candidatura explicitamente.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-3">
          <button
            type="button"
            onClick={() => onNavigate('resume')}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700"
          >
            <FileText className="h-4 w-4" />
            Gerenciar Currículo
          </button>
          <button
            type="button"
            onClick={() => onNavigate('opportunities')}
            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-500"
          >
            <Target className="h-4 w-4" />
            Painel de Oportunidades ({activeJobsCount})
          </button>
        </div>
      </div>

      {/* 6 Indicadores Oficiais do Dashboard Principal (Seção 9) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-6">
        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Vagas Encontradas
            </span>
            <Globe className="h-4 w-4 text-sky-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">{totalJobsFound}</p>
          <p className="mt-1 text-[11px] text-slate-400">
            {activeJobsCount} ativas no radar
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Analisadas pela IA
            </span>
            <Sparkles className="h-4 w-4 text-purple-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">
            {jobsAnalyzedByAi}
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Score 0 a 100% calculado
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Alta Compatibilidade
            </span>
            <Target className="h-4 w-4 text-emerald-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-emerald-300">
            {highCompatibilityJobs}
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Aderentes ao seu perfil
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Candidaturas Enviadas
            </span>
            <Send className="h-4 w-4 text-indigo-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">
            {applicationsSubmitted}
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Com aprovação explícita
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Entrevistas Agendadas
            </span>
            <CalendarCheck className="h-4 w-4 text-amber-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-amber-300">
            {interviewsScheduled}
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Processos avançando
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-medium uppercase tracking-wider">
              Taxa de Conversão
            </span>
            <TrendingUp className="h-4 w-4 text-emerald-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">
            {conversionRatePercent}%
          </p>
          <p className="mt-1 text-[11px] text-slate-400">
            Candidaturas / Vagas
          </p>
        </div>
      </div>

      {/* Status do Currículo e Evidências */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">
              Currículos Importados
            </span>
            <FileText className="h-4 w-4 text-indigo-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">{resumes.length}</p>
          <div className="mt-2 text-xs text-slate-400">
            {latestResume ? (
              <span className="inline-flex items-center gap-2">
                Último: <StatusBadge status={latestResume.status} />
              </span>
            ) : (
              'Nenhum arquivo enviado ainda'
            )}
          </div>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">
              Evidências Verificadas
            </span>
            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
          </div>
          <p className="mt-2 text-2xl font-bold text-white">
            {verifiedEvidences}
          </p>
          <p className="mt-2 text-xs text-slate-400">
            {reviewPendingEvidences} ponto(s) aguardando revisão
          </p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">
              Status do Perfil Profissional
            </span>
            <Briefcase className="h-4 w-4 text-purple-400" />
          </div>
          <p className="mt-2 text-xl font-bold text-white">
            {profile?.confirmed
              ? 'Confirmado'
              : profile?.fullName
                ? 'Aguardando Confirmação'
                : 'Não Estruturado'}
          </p>
          <p className="mt-2 text-xs text-slate-400">
            {profile?.skills.length ?? 0} competências catalogadas
          </p>
        </div>
      </div>

      {/* Pipeline Completo das 6 Etapas */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
        <h2 className="text-base font-semibold text-white">
          Jornada Completa do Candidato no CandidAUTO
        </h2>
        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-6">
          <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-slate-950/60 p-4">
            <div>
              <span className="text-xs font-semibold text-indigo-400">
                Etapa 1
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Currículo & Perfil IA
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Extração PDF/DOCX com evidências literais.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('resume')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-indigo-400 hover:text-indigo-300"
            >
              Ir para Currículo <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-slate-950/60 p-4">
            <div>
              <span className="text-xs font-semibold text-sky-400">
                Etapa 2
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Fontes & Vagas Reais
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Greenhouse, Lever, Gupy e Importação Assistida.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('jobs')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-sky-400 hover:text-sky-300"
            >
              Fontes & Vagas <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-slate-950/60 p-4">
            <div>
              <span className="text-xs font-semibold text-purple-400">
                Etapa 3
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Compatibilidade 0–100%
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Pontos positivos, atenção e filtros avançados.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('opportunities')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-purple-400 hover:text-purple-300"
            >
              Ver Oportunidades <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-slate-950/60 p-4">
            <div>
              <span className="text-xs font-semibold text-amber-400">
                Etapa 4
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Preenchimento IA
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Autopreenchimento fiel e cofre de respostas.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('applications')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-amber-400 hover:text-amber-300"
            >
              Preparar & Revisar <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-emerald-500/30 bg-emerald-950/20 p-4">
            <div>
              <span className="text-xs font-semibold text-emerald-400">
                Etapa 5
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Aprovação & Envio
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Botões CANDIDATAR-SE e CANCELAR com recibo.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('applications')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-emerald-400 hover:text-emerald-300"
            >
              Central Candidaturas <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="flex flex-col justify-between rounded-xl border border-indigo-500/30 bg-indigo-950/20 p-4">
            <div>
              <span className="text-xs font-semibold text-indigo-300">
                Etapa 6
              </span>
              <h3 className="mt-1 text-sm font-medium text-white">
                Automação & Radar
              </h3>
              <p className="mt-1 text-xs text-slate-400">
                Monitoramento periódico dos cargos desejados.
              </p>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('automation')}
              className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-indigo-300 hover:text-white"
            >
              Configurar Radar <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
