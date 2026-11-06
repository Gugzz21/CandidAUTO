import React, { useState } from 'react';
import {
  Sparkles,
  Filter,
  ExternalLink,
  CheckCircle2,
  AlertTriangle,
  Send,
  Building2,
  MapPin,
  DollarSign,
} from 'lucide-react';
import type {
  ApplicationStatus,
  JobPlatform,
  OpportunityCardDto,
  WorkModality,
} from '../../types/api';

interface OpportunitiesDashboardViewProps {
  opportunities: OpportunityCardDto[];
  isMatchingAll: boolean;
  preparingJobId: string | null;
  onRunMatchAll: () => Promise<void>;
  onPrepareApplication: (jobId: string) => Promise<void>;
}

const STATUS_LABELS: Record<ApplicationStatus, string> = {
  ENCONTRADA: 'Encontrada',
  EM_ANALISE_IA: 'Em análise pela IA',
  AGUARDANDO_REVISAO: 'Aguardando revisão',
  PRONTA_PARA_CANDIDATURA: 'Pronta para candidatura',
  CANDIDATURA_ENVIADA: 'Candidatura enviada',
  EM_ANDAMENTO: 'Em andamento',
  ENTREVISTA_AGENDADA: 'Entrevista agendada',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
  CANCELADA: 'Cancelada',
  VAGA_ENCERRADA: 'Vaga encerrada',
};

export const getScoreBadgeClass = (score: number) => {
  if (score >= 70) {
    return 'border-emerald-500/40 bg-emerald-950/60 text-emerald-400';
  }
  if (score >= 50) {
    return 'border-amber-500/40 bg-amber-950/60 text-amber-400';
  }
  return 'border-rose-500/40 bg-rose-950/60 text-rose-400';
};

export const OpportunitiesDashboardView: React.FC<
  OpportunitiesDashboardViewProps
> = ({
  opportunities,
  isMatchingAll,
  preparingJobId,
  onRunMatchAll,
  onPrepareApplication,
}) => {
  const [minScore, setMinScore] = useState<number>(0);
  const [areaFilter, setAreaFilter] = useState('');
  const [seniorityFilter, setSeniorityFilter] = useState('');
  const [companyFilter, setCompanyFilter] = useState('');
  const [salaryFilter, setSalaryFilter] = useState('');
  const [modalityFilter, setModalityFilter] = useState<WorkModality | ''>('');
  const [locationFilter, setLocationFilter] = useState('');
  const [platformFilter, setPlatformFilter] = useState<JobPlatform | ''>('');
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | ''>('');

  const filtered = opportunities.filter((item) => {
    const score = item.matchAnalysis?.compatibilityScore ?? 0;
    if (minScore > 0 && score < minScore) return false;

    if (areaFilter.trim()) {
      const q = areaFilter.toLowerCase();
      const text = `${item.job.title} ${item.job.technologies.join(' ')}`.toLowerCase();
      if (!text.includes(q)) return false;
    }

    if (seniorityFilter.trim()) {
      const q = seniorityFilter.toLowerCase();
      if (!(item.job.seniority ?? '').toLowerCase().includes(q)) return false;
    }

    if (companyFilter.trim()) {
      const q = companyFilter.toLowerCase();
      if (!item.job.companyName.toLowerCase().includes(q)) return false;
    }

    if (salaryFilter.trim()) {
      const q = salaryFilter.toLowerCase();
      if (!(item.job.salaryRange ?? '').toLowerCase().includes(q)) return false;
    }

    if (modalityFilter && item.job.workModality !== modalityFilter) {
      return false;
    }

    if (locationFilter.trim()) {
      const q = locationFilter.toLowerCase();
      if (!(item.job.location ?? '').toLowerCase().includes(q)) return false;
    }

    if (platformFilter && item.job.sourcePlatform !== platformFilter) {
      return false;
    }

    if (statusFilter && item.applicationStatus !== statusFilter) {
      return false;
    }

    return true;
  });


  return (
    <div className="space-y-6">
      {/* Header da Etapa 3 */}
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:flex-row sm:items-center">
        <div>
          <span className="eyebrow inline-flex items-center gap-2 text-slate-400">
            <Sparkles className="h-4 w-4" />
            Etapa 3 • Motor de Compatibilidade 0–100% Explicável
          </span>
          <h1 className="mt-2 text-2xl font-semibold text-white">
            Painel de Oportunidades Analisadas pela IA
          </h1>
          <p className="mt-1 text-base text-slate-400">
            Compare seu perfil com cada vaga encontrada, veja pontos positivos, pontos de atenção e prepare candidaturas inteligentes sem eliminar vagas por diferenciais desejáveis.
          </p>
        </div>

        <button
          type="button"
          data-testid="btn-match-all-jobs"
          disabled={isMatchingAll}
          onClick={() => void onRunMatchAll()}
          className="inline-flex shrink-0 items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-base font-medium text-signal-ink transition hover:bg-indigo-500 disabled:opacity-50"
        >
          <Sparkles className="h-[18px] w-[18px]" />
          {isMatchingAll
            ? 'Analisando Compatibilidade...'
            : 'Analisar Compatibilidade de Todas as Vagas'}
        </button>
      </div>

      {/* Barra com os 9 Filtros Solicitados */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-base font-semibold text-white">
            <Filter className="h-[18px] w-[18px] text-slate-400" />
            Filtros Avançados de Oportunidades
          </h2>
          <span className="text-sm text-slate-400">
            Exibindo {filtered.length} de {opportunities.length} oportunidade(s)
          </span>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-slate-400">
              1. Compatibilidade Mínima ({minScore}%)
            </label>
            <input
              type="range"
              min={0}
              max={95}
              step={5}
              value={minScore}
              onChange={(e) => setMinScore(Number(e.target.value))}
              className="mt-2 w-full accent-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              2. Área Profissional / Tecnologia
            </label>
            <input
              type="text"
              value={areaFilter}
              onChange={(e) => setAreaFilter(e.target.value)}
              placeholder="Ex: Full Stack, Back-end, React, Java..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              3. Senioridade
            </label>
            <input
              type="text"
              value={seniorityFilter}
              onChange={(e) => setSeniorityFilter(e.target.value)}
              placeholder="Ex: Júnior, Pleno, Sênior..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              4. Empresa
            </label>
            <input
              type="text"
              value={companyFilter}
              onChange={(e) => setCompanyFilter(e.target.value)}
              placeholder="Filtrar por empresa..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              5. Faixa Salarial
            </label>
            <input
              type="text"
              value={salaryFilter}
              onChange={(e) => setSalaryFilter(e.target.value)}
              placeholder="Ex: R$ 6.000..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              6. Modalidade
            </label>
            <select
              value={modalityFilter}
              onChange={(e) =>
                setModalityFilter(e.target.value as WorkModality | '')
              }
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            >
              <option value="">Todas as modalidades</option>
              <option value="REMOTO">Remoto</option>
              <option value="HIBRIDO">Híbrido</option>
              <option value="PRESENCIAL">Presencial</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              7. Localização
            </label>
            <input
              type="text"
              value={locationFilter}
              onChange={(e) => setLocationFilter(e.target.value)}
              placeholder="Ex: São Paulo, Remoto..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              8. Plataforma de Origem
            </label>
            <select
              value={platformFilter}
              onChange={(e) =>
                setPlatformFilter(e.target.value as JobPlatform | '')
              }
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            >
              <option value="">Todas as plataformas</option>
              <option value="GREENHOUSE">Greenhouse</option>
              <option value="LEVER">Lever</option>
              <option value="GUPY">Gupy</option>
              <option value="LINKEDIN">LinkedIn</option>
              <option value="INDEED">Indeed</option>
              <option value="GLASSDOOR">Glassdoor</option>
              <option value="PROGRAMATHOR">Programathor</option>
              <option value="REVELO">Revelo</option>
              <option value="INFOJOBS">InfoJobs</option>
              <option value="CAREER_PAGE">Página Oficial de Carreiras</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-400">
              9. Status da Candidatura
            </label>
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as ApplicationStatus | '')
              }
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white"
            >
              <option value="">Todos os status</option>
              {Object.entries(STATUS_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Cards de Oportunidades */}
      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/50 p-8 text-center">
          <p className="text-base text-slate-400">
            Nenhuma oportunidade encontrada para os filtros atuais. Importe ou sincronize vagas na aba "Fontes & Vagas" e clique em "Analisar Compatibilidade de Todas as Vagas".
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((card) => {
            const { job, matchAnalysis, applicationStatus } = card;
            const score = matchAnalysis?.compatibilityScore ?? null;
            return (
              <div
                key={job.id}
                data-testid={`opportunity-card-${job.id}`}
                className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6 transition hover:border-slate-700"
              >
                <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                  <div className="space-y-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-md border border-indigo-500/30 bg-indigo-500/10 px-2.5 py-0.5 text-sm font-medium text-indigo-300">
                        {job.sourcePlatform}
                      </span>
                      <span className="rounded-md border border-slate-700 bg-slate-850 px-2.5 py-0.5 text-sm text-slate-300">
                        {job.workModality}
                      </span>
                      <span className="rounded-md border border-slate-700 bg-slate-850 px-2.5 py-0.5 text-sm text-slate-300">
                        {job.seniority || 'Júnior/Pleno'}
                      </span>
                      <span className="rounded-md border border-purple-700/50 bg-purple-950/50 px-2.5 py-0.5 text-sm font-medium text-purple-300">
                        Status: {STATUS_LABELS[applicationStatus] ?? applicationStatus}
                      </span>
                    </div>

                    <h3 className="text-xl font-semibold text-white">
                      Vaga: {job.title}
                    </h3>

                    <div className="flex flex-wrap items-center gap-4 text-sm text-slate-400">
                      <span className="inline-flex items-center gap-1 font-medium text-slate-200">
                        <Building2 className="h-4 w-4 text-slate-400" />
                        {job.companyName}
                      </span>
                      {job.location && (
                        <span className="inline-flex items-center gap-1">
                          <MapPin className="h-4 w-4 text-slate-500" />
                          {job.location}
                        </span>
                      )}
                      {job.salaryRange && (
                        <span className="inline-flex items-center gap-1 text-emerald-300">
                          <DollarSign className="h-4 w-4" />
                          {job.salaryRange}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Badge de Compatibilidade 0-100% */}
                  <div className="flex items-center gap-3">
                    {score !== null ? (
                      <div
                        data-testid={`score-badge-${job.id}`}
                        className={`min-w-[112px] rounded-lg border px-4 py-2 text-left ${getScoreBadgeClass(
                          score
                        )}`}
                      >
                        <span className="block eyebrow opacity-80">
                          Compatibilidade
                        </span>
                        <span className="num text-3xl font-semibold">{score}%</span>
                      </div>
                    ) : (
                      <span className="rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-400">
                        Ainda não analisada
                      </span>
                    )}

                    <div className="flex flex-col gap-2">
                      <button
                        type="button"
                        data-testid={`btn-prepare-application-${job.id}`}
                        disabled={preparingJobId === job.id || !job.active}
                        onClick={() => void onPrepareApplication(job.id)}
                        className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-signal-ink transition hover:bg-indigo-500 disabled:opacity-50"
                      >
                        <Send className="h-4 w-4" />
                        {preparingJobId === job.id
                          ? 'Preparando...'
                          : 'Preparar Candidatura com IA'}
                      </button>

                      <a
                        href={job.originalUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center justify-center gap-1 rounded-xl border border-slate-700 bg-slate-850 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-700"
                      >
                        Publicação Original <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    </div>
                  </div>
                </div>

                {/* Análise Explicável da IA */}
                {matchAnalysis && (
                  <div className="mt-4 space-y-3 rounded-xl border border-slate-800/90 bg-slate-950/60 p-4">
                    <p className="text-sm leading-relaxed text-slate-200">
                      <strong className="text-indigo-300">Resumo da IA:</strong>{' '}
                      {matchAnalysis.aiSummary}
                    </p>

                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                      {/* Pontos Positivos & Requisitos Atendidos */}
                      <div className="space-y-1.5">
                        <h4 className="flex items-center gap-1.5 text-sm font-semibold text-emerald-400">
                          <CheckCircle2 className="h-4 w-4" />
                          Pontos Positivos & Requisitos Atendidos
                        </h4>
                        <ul className="space-y-1 text-sm text-slate-300">
                          {matchAnalysis.positivePoints.map((pt, idx) => (
                            <li key={idx} className="flex items-start gap-1.5">
                              <span className="text-emerald-400">•</span>
                              <span>{pt}</span>
                            </li>
                          ))}
                          {matchAnalysis.matchedRequirements.map((req, idx) => (
                            <li
                              key={`req-${idx}`}
                              className="flex items-start gap-1.5 text-slate-400"
                            >
                              <span className="text-emerald-500">✓</span>
                              <span>{req}</span>
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Pontos de Atenção & Requisitos Não Atendidos */}
                      <div className="space-y-1.5">
                        <h4 className="flex items-center gap-1.5 text-sm font-semibold text-amber-400">
                          <AlertTriangle className="h-4 w-4" />
                          Pontos de Atenção & Diferenciais
                        </h4>
                        {matchAnalysis.attentionPoints.length === 0 &&
                        matchAnalysis.missingRequirements.length === 0 ? (
                          <p className="text-sm text-slate-400">
                            Nenhum ponto crítico de atenção identificado.
                          </p>
                        ) : (
                          <ul className="space-y-1 text-sm text-slate-300">
                            {matchAnalysis.attentionPoints.map((pt, idx) => (
                              <li key={idx} className="flex items-start gap-1.5">
                                <span className="text-amber-400">•</span>
                                <span>{pt}</span>
                              </li>
                            ))}
                            {matchAnalysis.missingRequirements.map(
                              (req, idx) => (
                                <li
                                  key={`miss-${idx}`}
                                  className="flex items-start gap-1.5 text-slate-400"
                                >
                                  <span className="text-rose-400">!</span>
                                  <span>{req}</span>
                                </li>
                              )
                            )}
                          </ul>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
