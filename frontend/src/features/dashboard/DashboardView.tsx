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

  const kpis: {
    label: string;
    value: string | number;
    hint: string;
    icon: React.FC<{ className?: string; strokeWidth?: number }>;
    tone?: string;
  }[] = [
    { label: 'Vagas encontradas', value: totalJobsFound, hint: `${activeJobsCount} ativas no radar`, icon: Globe },
    { label: 'Analisadas pela IA', value: jobsAnalyzedByAi, hint: 'Score 0–100 calculado', icon: Sparkles },
    { label: 'Alta compatibilidade', value: highCompatibilityJobs, hint: 'Aderentes ao seu perfil', icon: Target, tone: 'text-emerald-400' },
    { label: 'Candidaturas enviadas', value: applicationsSubmitted, hint: 'Com aprovação explícita', icon: Send },
    { label: 'Entrevistas', value: interviewsScheduled, hint: 'Processos avançando', icon: CalendarCheck, tone: 'text-amber-400' },
    { label: 'Conversão', value: `${conversionRatePercent}%`, hint: 'Candidaturas / vagas', icon: TrendingUp },
  ];

  const steps: {
    n: string;
    title: string;
    desc: string;
    cta: string;
    tab: NavTabId;
  }[] = [
    { n: '01', title: 'Currículo e perfil', desc: 'Extração PDF/DOCX com evidências literais.', cta: 'Abrir currículo', tab: 'resume' },
    { n: '02', title: 'Fontes e vagas', desc: 'Greenhouse, Lever, Gupy e importação assistida.', cta: 'Ver fontes', tab: 'jobs' },
    { n: '03', title: 'Compatibilidade', desc: 'Score 0–100 com o porquê de cada nota.', cta: 'Triar vagas', tab: 'opportunities' },
    { n: '04', title: 'Preenchimento', desc: 'Só usa dados que você já confirmou.', cta: 'Preparar', tab: 'applications' },
    { n: '05', title: 'Aprovação', desc: 'Nada é enviado sem sua confirmação.', cta: 'Revisar envios', tab: 'applications' },
    { n: '06', title: 'Radar', desc: 'Monitoramento periódico dos cargos-alvo.', cta: 'Configurar', tab: 'automation' },
  ];

  return (
    <div className="space-y-8">
      <header className="flex flex-col justify-between gap-6 lg:flex-row lg:items-end">
        <div className="max-w-2xl">
          <span className="eyebrow inline-flex items-center gap-2 text-slate-400">
            <ShieldCheck className="h-3.5 w-3.5" strokeWidth={1.75} />
            Painel de triagem
          </span>
          <h1 className="mt-3 text-3xl font-semibold text-white sm:text-4xl">
            Vagas certas, com o porquê.
          </h1>
          <p className="mt-3 text-base text-slate-400">
            A IA lê seu currículo sem inventar nada, cruza com vagas reais e explica cada nota de compatibilidade. Nenhuma candidatura sai sem sua confirmação.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2">
          <button
            type="button"
            onClick={() => onNavigate('resume')}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-850 px-4 py-2.5 text-sm font-medium text-white hover:border-slate-600 hover:bg-slate-800"
          >
            <FileText className="h-4 w-4" strokeWidth={1.75} />
            Gerenciar currículo
          </button>
          <button
            type="button"
            onClick={() => onNavigate('opportunities')}
            className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-signal-ink hover:bg-indigo-500"
          >
            <Target className="h-4 w-4" strokeWidth={1.75} />
            Triar oportunidades
            <span className="num rounded bg-signal-ink/15 px-1.5 text-xs">{activeJobsCount}</span>
          </button>
        </div>
      </header>

      {/* 6 Indicadores Oficiais do Dashboard Principal (Seção 9) */}
      <section aria-label="Indicadores" className="grid grid-cols-2 overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 md:grid-cols-3 xl:grid-cols-6">
        {kpis.map((k) => {
          const Icon = k.icon;
          return (
            <div key={k.label} className="-mb-px -mr-px border-b border-r border-slate-800 p-6">
              <div className="flex items-center justify-between gap-2 text-slate-400">
                <span className="eyebrow">{k.label}</span>
                <Icon className="h-[18px] w-[18px] shrink-0" strokeWidth={1.75} />
              </div>
              <p className={`num mt-3 text-4xl font-semibold ${k.tone ?? 'text-white'}`}>
                {k.value}
              </p>
              <p className="mt-1.5 text-sm text-slate-400">{k.hint}</p>
            </div>
          );
        })}
      </section>

      {/* Status do Currículo e Evidências */}
      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
          <div className="flex items-center justify-between text-slate-400">
            <span className="eyebrow">Currículos importados</span>
            <FileText className="h-4 w-4" strokeWidth={1.75} />
          </div>
          <p className="num mt-3 text-3xl font-semibold text-white">{resumes.length}</p>
          <div className="mt-2 text-sm text-slate-400">
            {latestResume ? (
              <span className="inline-flex items-center gap-2">
                Último: <StatusBadge status={latestResume.status} />
              </span>
            ) : (
              'Nenhum arquivo enviado ainda'
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
          <div className="flex items-center justify-between text-slate-400">
            <span className="eyebrow">Evidências verificadas</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-400" strokeWidth={1.75} />
          </div>
          <p className="num mt-3 text-3xl font-semibold text-white">{verifiedEvidences}</p>
          <p className="mt-2 text-sm text-slate-400">
            <span className="num text-amber-400">{reviewPendingEvidences}</span> ponto(s) aguardando revisão
          </p>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
          <div className="flex items-center justify-between text-slate-400">
            <span className="eyebrow">Perfil profissional</span>
            <Briefcase className="h-4 w-4" strokeWidth={1.75} />
          </div>
          <p className="mt-3 font-display text-2xl font-semibold tracking-tight text-white">
            {profile?.confirmed
              ? 'Confirmado'
              : profile?.fullName
                ? 'Aguardando confirmação'
                : 'Não estruturado'}
          </p>
          <p className="mt-2 text-sm text-slate-400">
            <span className="num">{profile?.skills.length ?? 0}</span> competências catalogadas
          </p>
        </div>
      </section>

      {/* Pipeline Completo das 6 Etapas */}
      <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
        <div className="flex items-baseline justify-between gap-4">
          <h2 className="text-xl font-semibold text-white">Fluxo de trabalho</h2>
          <span className="eyebrow text-slate-400">6 etapas</span>
        </div>
        <ol className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-6">
          {steps.map((s) => (
            <li
              key={s.n}
              className="group flex flex-col justify-between rounded-lg border border-slate-800 bg-slate-950/60 p-4 transition-colors hover:border-slate-700"
            >
              <div>
                <span className="num text-sm text-slate-500">{s.n}</span>
                <h3 className="mt-2 text-base font-semibold text-white">{s.title}</h3>
                <p className="mt-1 text-sm text-slate-400">{s.desc}</p>
              </div>
              <button
                type="button"
                onClick={() => onNavigate(s.tab)}
                className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-slate-300 hover:text-indigo-400"
              >
                {s.cta}
                <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" strokeWidth={1.75} />
              </button>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
};
