import React, { useState } from 'react';
import {
  Globe,
  Plus,
  RefreshCw,
  Trash2,
  ExternalLink,
  Building2,
  MapPin,
  Briefcase,
  CheckCircle2,
  XCircle,
  Loader2,
  Link2,
  Search,
} from 'lucide-react';
import type {
  CreateJobSourceRequestDto,
  ImportJobRequestDto,
  JobPlatform,
  JobPostingDto,
  JobSourceDto,
  WorkModality,
} from '../../types/api';

interface JobDiscoveryViewProps {
  sources: JobSourceDto[];
  jobs: JobPostingDto[];
  syncingSourceId: string | null;
  onCreateSource: (payload: CreateJobSourceRequestDto) => Promise<void>;
  onDeleteSource: (id: string) => Promise<void>;
  onSyncSource: (id: string) => Promise<void>;
  onImportJob: (payload: ImportJobRequestDto) => Promise<void>;
  onToggleAvailability: (id: string, active: boolean) => Promise<void>;
}

const MODALITY_LABELS: Record<WorkModality, string> = {
  REMOTO: 'Remoto',
  HIBRIDO: 'Híbrido',
  PRESENCIAL: 'Presencial',
  NAO_INFORMADO: 'Não informado',
};

export const JobDiscoveryView: React.FC<JobDiscoveryViewProps> = ({
  sources,
  jobs,
  syncingSourceId,
  onCreateSource,
  onDeleteSource,
  onSyncSource,
  onImportJob,
  onToggleAvailability,
}) => {
  // Estado para novo Conector de Fonte
  const [sourceName, setSourceName] = useState('');
  const [sourcePlatform, setSourcePlatform] =
    useState<JobPlatform>('GREENHOUSE');
  const [boardIdentifier, setBoardIdentifier] = useState('');
  const [isCreatingSource, setIsCreatingSource] = useState(false);

  // Estado para Importação Assistida de Vaga (LinkedIn, Indeed, Gupy, etc.)
  const [showImportModal, setShowImportModal] = useState(false);
  const [impPlatform, setImpPlatform] = useState<JobPlatform>('LINKEDIN');
  const [impCompany, setImpCompany] = useState('');
  const [impTitle, setImpTitle] = useState('');
  const [impLocation, setImpLocation] = useState('');
  const [impModality, setImpModality] = useState<WorkModality>('REMOTO');
  const [impSeniority, setImpSeniority] = useState('Júnior');
  const [impSalary, setImpSalary] = useState('');
  const [impUrl, setImpUrl] = useState('');
  const [impDescription, setImpDescription] = useState('');
  const [isImporting, setIsImporting] = useState(false);

  // Filtros de visualização
  const [searchQuery, setSearchQuery] = useState('');
  const [filterPlatform, setFilterPlatform] = useState<string>('ALL');
  const [filterModality, setFilterModality] = useState<string>('ALL');
  const [showExpired, setShowExpired] = useState(false);

  const handleAddSource = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!sourceName.trim() || !boardIdentifier.trim()) return;
    setIsCreatingSource(true);
    try {
      await onCreateSource({
        name: sourceName.trim(),
        platform: sourcePlatform,
        boardIdentifier: boardIdentifier.trim(),
      });
      setSourceName('');
      setBoardIdentifier('');
    } finally {
      setIsCreatingSource(false);
    }
  };

  const handleImportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (
      !impCompany.trim() ||
      !impTitle.trim() ||
      !impUrl.trim() ||
      !impDescription.trim()
    ) {
      return;
    }
    setIsImporting(true);
    try {
      await onImportJob({
        sourcePlatform: impPlatform,
        companyName: impCompany.trim(),
        title: impTitle.trim(),
        location: impLocation.trim(),
        workModality: impModality,
        seniority: impSeniority.trim(),
        salaryRange: impSalary.trim(),
        originalUrl: impUrl.trim(),
        descriptionRaw: impDescription.trim(),
      });
      setImpCompany('');
      setImpTitle('');
      setImpLocation('');
      setImpSalary('');
      setImpUrl('');
      setImpDescription('');
      setShowImportModal(false);
    } finally {
      setIsImporting(false);
    }
  };

  const filteredJobs = jobs.filter((job) => {
    if (!showExpired && !job.active) return false;
    if (filterPlatform !== 'ALL' && job.sourcePlatform !== filterPlatform) {
      return false;
    }
    if (filterModality !== 'ALL' && job.workModality !== filterModality) {
      return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const target = `${job.title} ${job.companyName} ${job.location} ${job.technologies.join(' ')}`.toLowerCase();
      if (!target.includes(q)) return false;
    }
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Cabeçalho da Etapa 2 */}
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:flex-row sm:items-center">
        <div>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 text-xs font-medium text-indigo-300">
            <Globe className="h-3.5 w-3.5" />
            Etapa 2 • Motor de Descoberta & Conectores Reais
          </span>
          <h2 className="mt-2 text-lg font-semibold text-white">
            Fontes de Emprego, Normalização e Deduplicação de Vagas
          </h2>
          <p className="mt-1 text-sm text-slate-400">
            Sincronize quadros públicos via API oficial (Greenhouse, Lever, Gupy / Career Pages JSON-LD) ou importe vagas assistidas do LinkedIn, Indeed, Programathor e Revelo.
          </p>
        </div>

        <button
          type="button"
          data-testid="toggle-import-job-btn"
          onClick={() => setShowImportModal((v) => !v)}
          className="inline-flex shrink-0 items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-indigo-500"
        >
          <Link2 className="h-4 w-4" />
          {showImportModal ? 'Fechar Importador' : 'Importar Vaga por Link'}
        </button>
      </div>

      {/* Formulário de Importação Assistida (LinkedIn / Indeed / Gupy / Glassdoor / Programathor) */}
      {showImportModal && (
        <form
          onSubmit={handleImportSubmit}
          data-testid="import-job-form"
          className="space-y-4 rounded-2xl border border-indigo-500/40 bg-slate-900/90 p-6"
        >
          <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
            Importar Vaga Assistida (Sem violar termos de uso)
          </h3>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Plataforma de Origem *
              </label>
              <select
                value={impPlatform}
                onChange={(e) => setImpPlatform(e.target.value as JobPlatform)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              >
                <option value="LINKEDIN">LinkedIn Jobs</option>
                <option value="INDEED">Indeed</option>
                <option value="GUPY">Gupy</option>
                <option value="GLASSDOOR">Glassdoor</option>
                <option value="PROGRAMATHOR">Programathor</option>
                <option value="REVELO">Revelo</option>
                <option value="INFOJOBS">InfoJobs</option>
                <option value="GREENHOUSE">Greenhouse</option>
                <option value="LEVER">Lever</option>
                <option value="CAREER_PAGE">Página Oficial de Carreiras</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Nome da Empresa *
              </label>
              <input
                data-testid="import-company-input"
                type="text"
                value={impCompany}
                onChange={(e) => setImpCompany(e.target.value)}
                placeholder="Ex.: Mercado Livre"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Título da Vaga *
              </label>
              <input
                data-testid="import-title-input"
                type="text"
                value={impTitle}
                onChange={(e) => setImpTitle(e.target.value)}
                placeholder="Ex.: Desenvolvedor Full Stack Júnior"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Modalidade
              </label>
              <select
                value={impModality}
                onChange={(e) => setImpModality(e.target.value as WorkModality)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              >
                <option value="REMOTO">Remoto</option>
                <option value="HIBRIDO">Híbrido</option>
                <option value="PRESENCIAL">Presencial</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Senioridade
              </label>
              <input
                type="text"
                value={impSeniority}
                onChange={(e) => setImpSeniority(e.target.value)}
                placeholder="Júnior / Pleno / Sênior"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Localização
              </label>
              <input
                type="text"
                value={impLocation}
                onChange={(e) => setImpLocation(e.target.value)}
                placeholder="Ex.: São Paulo, SP"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Faixa Salarial (se disponível)
              </label>
              <input
                type="text"
                value={impSalary}
                onChange={(e) => setImpSalary(e.target.value)}
                placeholder="Ex.: R$ 5.500 - R$ 7.000"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300">
              Link Original da Oportunidade *
            </label>
            <input
              data-testid="import-url-input"
              type="url"
              value={impUrl}
              onChange={(e) => setImpUrl(e.target.value)}
              placeholder="https://..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300">
              Descrição Completa da Vaga * (O sistema extrairá requisitos obrigatórios, desejáveis, tecnologias e benefícios)
            </label>
            <textarea
              data-testid="import-description-input"
              rows={4}
              value={impDescription}
              onChange={(e) => setImpDescription(e.target.value)}
              placeholder="Cole aqui a descrição completa, requisitos obrigatórios, diferenciais e benefícios..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
            />
          </div>

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowImportModal(false)}
              className="rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-xs font-medium text-slate-300"
            >
              Cancelar
            </button>
            <button
              type="submit"
              data-testid="submit-import-job-btn"
              disabled={isImporting}
              className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50"
            >
              {isImporting ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : (
                <Plus className="h-3.5 w-3.5" />
              )}
              Normalizar e Salvar Vaga
            </button>
          </div>
        </form>
      )}

      {/* Conectores de Fontes Automáticas (Greenhouse, Lever, Gupy, Career Pages) */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
          Conectores de Fontes Automáticas ({sources.length})
        </h3>

        <form
          onSubmit={handleAddSource}
          className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-12"
        >
          <input
            type="text"
            value={sourceName}
            onChange={(e) => setSourceName(e.target.value)}
            placeholder="Nome da Empresa (ex.: Nubank, Brex, GitLab)"
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white sm:col-span-4"
          />
          <select
            value={sourcePlatform}
            onChange={(e) => setSourcePlatform(e.target.value as JobPlatform)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white sm:col-span-3"
          >
            <option value="GREENHOUSE">Greenhouse (Job Board API)</option>
            <option value="LEVER">Lever (Postings API)</option>
            <option value="GUPY">Gupy (Portal Público)</option>
            <option value="CAREER_PAGE">Career Page (JobPosting JSON-LD)</option>
          </select>
          <input
            type="text"
            value={boardIdentifier}
            onChange={(e) => setBoardIdentifier(e.target.value)}
            placeholder="Board Token ou URL (ex.: nubank, brex, https://...)"
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white sm:col-span-3"
          />
          <button
            type="submit"
            disabled={isCreatingSource}
            className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-2 text-xs font-medium text-white hover:bg-indigo-500 sm:col-span-2"
          >
            <Plus className="h-3.5 w-3.5" />
            Adicionar Fonte
          </button>
        </form>

        {sources.length > 0 && (
          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            {sources.map((src) => {
              const isSyncing = syncingSourceId === src.id;
              return (
                <div
                  key={src.id}
                  className="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-950/60 p-3.5"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-white">
                        {src.name}
                      </span>
                      <span className="rounded border border-slate-700 bg-slate-900 px-2 py-0.5 text-[10px] font-semibold text-indigo-300">
                        {src.platform}
                      </span>
                    </div>
                    <p className="mt-0.5 text-xs text-slate-400">
                      Board: <code className="text-slate-300">{src.boardIdentifier}</code>
                    </p>
                    {src.lastSyncStatus && (
                      <p className="mt-1 text-[11px] text-slate-400">
                        {src.lastSyncStatus}
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={isSyncing}
                      onClick={() => onSyncSource(src.id)}
                      className="inline-flex items-center gap-1 rounded-lg bg-indigo-600/90 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
                    >
                      <RefreshCw
                        className={`h-3.5 w-3.5 ${isSyncing ? 'animate-spin' : ''}`}
                      />
                      {isSyncing ? 'Sincronizando...' : 'Sincronizar'}
                    </button>
                    <button
                      type="button"
                      onClick={() => onDeleteSource(src.id)}
                      className="rounded-lg border border-slate-800 p-1.5 text-slate-400 hover:text-rose-400"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Barra de Filtros das Vagas Encontradas */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filtrar por cargo, empresa, tecnologia ou cidade..."
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-1.5 text-xs text-white focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select
            value={filterPlatform}
            onChange={(e) => setFilterPlatform(e.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs text-white"
          >
            <option value="ALL">Todas as Plataformas</option>
            <option value="GREENHOUSE">Greenhouse</option>
            <option value="LEVER">Lever</option>
            <option value="GUPY">Gupy</option>
            <option value="LINKEDIN">LinkedIn</option>
            <option value="INDEED">Indeed</option>
            <option value="CAREER_PAGE">Páginas de Carreiras</option>
          </select>

          <select
            value={filterModality}
            onChange={(e) => setFilterModality(e.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs text-white"
          >
            <option value="ALL">Todas as Modalidades</option>
            <option value="REMOTO">Remoto</option>
            <option value="HIBRIDO">Híbrido</option>
            <option value="PRESENCIAL">Presencial</option>
          </select>

          <label className="inline-flex items-center gap-1.5 text-xs text-slate-300">
            <input
              type="checkbox"
              checked={showExpired}
              onChange={(e) => setShowExpired(e.target.checked)}
              className="rounded border-slate-700 bg-slate-950"
            />
            Exibir expiradas
          </label>
        </div>
      </div>

      {/* Lista de Oportunidades Normalizadas */}
      <div className="space-y-4">
        {filteredJobs.length === 0 ? (
          <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-10 text-center text-sm text-slate-400">
            Nenhuma vaga encontrada com os filtros atuais. Adicione uma fonte acima ou clique em "Importar Vaga por Link".
          </div>
        ) : (
          filteredJobs.map((job) => (
            <div
              key={job.id}
              data-testid={`job-card-${job.id}`}
              className={`rounded-2xl border p-5 transition ${
                job.active
                  ? 'border-slate-800 bg-slate-900/70'
                  : 'border-slate-800/50 bg-slate-950/50 opacity-70'
              }`}
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-md border border-indigo-500/30 bg-indigo-500/10 px-2 py-0.5 text-[11px] font-semibold text-indigo-300">
                      {job.sourcePlatform}
                    </span>
                    <span className="rounded-md border border-slate-700 bg-slate-800 px-2 py-0.5 text-[11px] text-slate-300">
                      {MODALITY_LABELS[job.workModality]}
                    </span>
                    {job.seniority && (
                      <span className="rounded-md border border-purple-500/30 bg-purple-500/10 px-2 py-0.5 text-[11px] text-purple-300">
                        {job.seniority}
                      </span>
                    )}
                    {!job.active && (
                      <span className="rounded-md border border-rose-700 bg-rose-950 px-2 py-0.5 text-[11px] text-rose-300">
                        Vaga Expirada / Indisponível
                      </span>
                    )}
                  </div>
                  <h3 className="mt-2 text-base font-semibold text-white">
                    {job.title}
                  </h3>
                  <div className="mt-1 flex flex-wrap items-center gap-4 text-xs text-slate-400">
                    <span className="inline-flex items-center gap-1">
                      <Building2 className="h-3.5 w-3.5 text-slate-500" />
                      {job.companyName}
                    </span>
                    {job.location && (
                      <span className="inline-flex items-center gap-1">
                        <MapPin className="h-3.5 w-3.5 text-slate-500" />
                        {job.location}
                      </span>
                    )}
                    {job.salaryRange && (
                      <span className="inline-flex items-center gap-1 text-emerald-300">
                        <Briefcase className="h-3.5 w-3.5" />
                        {job.salaryRange}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => onToggleAvailability(job.id, !job.active)}
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-300 hover:bg-slate-700"
                  >
                    {job.active ? (
                      <>
                        <XCircle className="h-3.5 w-3.5 text-amber-400" />
                        Marcar Expirada
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400" />
                        Reativar
                      </>
                    )}
                  </button>
                  {job.originalUrl && (
                    <a
                      href={job.originalUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-500"
                    >
                      Link Original <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  )}
                </div>
              </div>

              {/* Tecnologias extraídas */}
              {job.technologies.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {job.technologies.map((tech) => (
                    <span
                      key={tech}
                      className="rounded border border-slate-700 bg-slate-950 px-2 py-0.5 text-[11px] font-medium text-slate-200"
                    >
                      {tech}
                    </span>
                  ))}
                </div>
              )}

              {/* Requisitos obrigatórios e desejáveis */}
              <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
                <div className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-3">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-indigo-400">
                    Requisitos Obrigatórios ({job.mandatoryRequirements.length})
                  </span>
                  {job.mandatoryRequirements.length === 0 ? (
                    <p className="mt-1 text-xs text-slate-400">
                      Consulte a descrição completa.
                    </p>
                  ) : (
                    <ul className="mt-1.5 list-inside list-disc space-y-1 text-xs text-slate-300">
                      {job.mandatoryRequirements.slice(0, 5).map((req, idx) => (
                        <li key={idx}>{req}</li>
                      ))}
                    </ul>
                  )}
                </div>

                <div className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-3">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-purple-400">
                    Requisitos Desejáveis / Benefícios
                  </span>
                  {job.desirableRequirements.length > 0 ? (
                    <ul className="mt-1.5 list-inside list-disc space-y-1 text-xs text-slate-300">
                      {job.desirableRequirements.slice(0, 4).map((req, idx) => (
                        <li key={idx}>{req}</li>
                      ))}
                    </ul>
                  ) : (
                    <p className="mt-1 text-xs text-slate-400">
                      Sem diferenciais separados listados.
                    </p>
                  )}
                  {job.benefits.length > 0 && (
                    <p className="mt-2 text-[11px] text-emerald-300">
                      Benefícios: {job.benefits.join(' • ')}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
