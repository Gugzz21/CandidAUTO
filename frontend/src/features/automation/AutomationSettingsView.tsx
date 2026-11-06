import React, { useEffect, useState } from 'react';
import {
  Bot,
  Play,
  Plus,
  Trash2,
  Save,
  CheckCircle2,
  Sliders,
} from 'lucide-react';
import type {
  SearchConfigDto,
  UpdateSearchConfigRequestDto,
} from '../../types/api';
import { ConnectorDiagnosticsTable } from '../jobs/ConnectorDiagnosticsTable';

interface AutomationSettingsViewProps {
  searchConfig: SearchConfigDto | null;
  isSaving: boolean;
  isRunningCycle: boolean;
  onSaveConfig: (payload: UpdateSearchConfigRequestDto) => Promise<void>;
  onRunMonitoringNow: () => Promise<void>;
}

export const AutomationSettingsView: React.FC<AutomationSettingsViewProps> = ({
  searchConfig,
  isSaving,
  isRunningCycle,
  onSaveConfig,
  onRunMonitoringNow,
}) => {
  const [desiredRoles, setDesiredRoles] = useState<string[]>([
    'Desenvolvedor Front-end Júnior',
    'Desenvolvedor Back-end Júnior',
    'Desenvolvedor Full Stack Júnior',
  ]);
  const [newRoleInput, setNewRoleInput] = useState('');
  const [desiredModalities, setDesiredModalities] = useState<string[]>([
    'REMOTO',
    'HIBRIDO',
  ]);
  const [desiredSeniorities, setDesiredSeniorities] = useState<string[]>([
    'Júnior',
    'Pleno',
  ]);
  const [minScore, setMinScore] = useState<number>(70);
  const [autoEnabled, setAutoEnabled] = useState<boolean>(true);
  const [intervalMinutes, setIntervalMinutes] = useState<number>(1440);

  useEffect(() => {
    if (searchConfig) {
      setDesiredRoles(searchConfig.desiredRoles);
      setDesiredModalities(searchConfig.desiredModalities);
      setDesiredSeniorities(searchConfig.desiredSeniorities);
      setMinScore(searchConfig.minCompatibilityScore);
      setAutoEnabled(searchConfig.autoMonitoringEnabled);
      setIntervalMinutes(searchConfig.intervalMinutes);
    }
  }, [searchConfig]);

  const handleAddRole = () => {
    const val = newRoleInput.trim();
    if (!val || desiredRoles.includes(val)) return;
    setDesiredRoles((prev) => [...prev, val]);
    setNewRoleInput('');
  };

  const handleRemoveRole = (role: string) => {
    setDesiredRoles((prev) => prev.filter((r) => r !== role));
  };

  const toggleModality = (mod: string) => {
    setDesiredModalities((prev) =>
      prev.includes(mod) ? prev.filter((m) => m !== mod) : [...prev, mod]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onSaveConfig({
      desiredRoles,
      desiredModalities,
      desiredSeniorities,
      minCompatibilityScore: minScore,
      autoMonitoringEnabled: autoEnabled,
      intervalMinutes,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:flex-row sm:items-center">
        <div>
          <span className="eyebrow inline-flex items-center gap-2 text-slate-400">
            <Bot className="h-4 w-4" />
            Etapa 6 • Automação e Monitoramento Inteligente de Vagas
          </span>
          <h1 className="mt-2 text-2xl font-semibold text-white">
            Configurações de Pesquisa e Automação
          </h1>
          <p className="mt-1 text-base text-slate-400">
            O agendador em segundo plano executa periodicamente o Job Discovery Engine conforme o intervalo configurado, consultando APIs públicas (Gupy Portal Público, Remotive, Arbeitnow, Catálogo Greenhouse & Lever) e suas fontes cadastradas com filtragem automática por cargo, modalidade e senioridade.
          </p>
        </div>

        <button
          type="button"
          data-testid="btn-run-monitoring-now"
          disabled={isRunningCycle}
          onClick={() => void onRunMonitoringNow()}
          className="inline-flex shrink-0 items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-base font-semibold text-signal-ink transition hover:bg-indigo-500 disabled:opacity-50"
        >
          <Play className="h-[18px] w-[18px]" />
          {isRunningCycle
            ? 'Executando Ciclo...'
            : 'Executar Monitoramento Agora'}
        </button>
      </div>

      {searchConfig?.lastRunSummary && (
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-xl border border-emerald-800/60 bg-emerald-950/30 p-4 text-sm text-emerald-200">
            <CheckCircle2 className="mt-0.5 h-[18px] w-[18px] shrink-0 text-emerald-400" />
            <div>
              <p className="font-semibold">
                Último Ciclo de Monitoramento
                {searchConfig.lastRunAt
                  ? ` (${new Date(searchConfig.lastRunAt).toLocaleString('pt-BR')})`
                  : ''}
                :
              </p>
              <p className="mt-0.5 text-emerald-300/90">
                {searchConfig.lastRunSummary}
              </p>
            </div>
          </div>

          {searchConfig.lastExecutionDiagnostics &&
            searchConfig.lastExecutionDiagnostics.length > 0 && (
              <ConnectorDiagnosticsTable
                diagnostics={searchConfig.lastExecutionDiagnostics}
                title="Telemetria do Último Ciclo Automático"
              />
            )}
        </div>
      )}

      <form
        onSubmit={handleSubmit}
        className="space-y-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-6"
      >
        <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
          <Sliders className="h-[18px] w-[18px] text-slate-400" />
          <h2 className="text-base font-semibold text-white">
            Parâmetros de Busca Automática
          </h2>
        </div>

        {/* Cargos Procurados */}
        <div>
          <label className="block text-sm font-medium text-slate-300">
            Cargos e Oportunidades que Você Procura
          </label>
          <div className="mt-2 flex gap-2">
            <input
              type="text"
              value={newRoleInput}
              onChange={(e) => setNewRoleInput(e.target.value)}
              placeholder="Ex: Desenvolvedor Full Stack Júnior..."
              className="flex-1 rounded-xl border border-slate-700 bg-slate-950 px-3.5 py-2 text-sm text-white"
            />
            <button
              type="button"
              onClick={handleAddRole}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-700 bg-slate-850 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
            >
              <Plus className="h-4 w-4" />
              Adicionar Cargo
            </button>
          </div>

          <div className="mt-3 flex flex-wrap gap-2">
            {desiredRoles.map((role) => (
              <span
                key={role}
                className="inline-flex items-center gap-2 rounded-xl border border-indigo-500/40 bg-indigo-950/40 px-3 py-1.5 text-sm text-indigo-200"
              >
                {role}
                <button
                  type="button"
                  onClick={() => handleRemoveRole(role)}
                  className="text-slate-400 hover:text-rose-400"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </span>
            ))}
          </div>
        </div>

        {/* Modalidades e Score Mínimo */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          <div>
            <label className="block text-sm font-medium text-slate-300">
              Modalidades Desejadas
            </label>
            <div className="mt-2 flex flex-wrap gap-2">
              {['REMOTO', 'HIBRIDO', 'PRESENCIAL'].map((mod) => {
                const active = desiredModalities.includes(mod);
                return (
                  <button
                    key={mod}
                    type="button"
                    onClick={() => toggleModality(mod)}
                    className={`rounded-xl border px-3 py-1.5 text-sm font-medium transition ${
                      active
                        ? 'border-indigo-500 bg-indigo-600 text-signal-ink'
                        : 'border-slate-800 bg-slate-950 text-slate-400'
                    }`}
                  >
                    {mod}
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300">
              Compatibilidade Mínima de Destaque ({minScore}%)
            </label>
            <input
              type="range"
              min={40}
              max={95}
              step={5}
              value={minScore}
              onChange={(e) => setMinScore(Number(e.target.value))}
              className="mt-3 w-full accent-indigo-500"
            />
          </div>

          <div>
            <div className="flex items-center justify-between">
              <label className="block text-sm font-medium text-slate-300">
                Frequência de Varredura
              </label>
              <span className="text-sm font-semibold text-indigo-300">
                {intervalMinutes >= 1440
                  ? 'Atualização Diária (a cada 24h)'
                  : intervalMinutes >= 60
                  ? `A cada ${Math.round(intervalMinutes / 60)}h (${intervalMinutes} min)`
                  : `A cada ${intervalMinutes} min`}
              </span>
            </div>

            <div className="mt-2 flex flex-wrap gap-1.5">
              {[
                { label: '1 Hora', mins: 60 },
                { label: '6 Horas', mins: 360 },
                { label: '12 Horas', mins: 720 },
                { label: 'Diário (24h)', mins: 1440 },
              ].map((preset) => (
                <button
                  key={preset.mins}
                  type="button"
                  onClick={() => setIntervalMinutes(preset.mins)}
                  className={`rounded-lg border px-2.5 py-1 text-sm font-medium transition ${
                    intervalMinutes === preset.mins
                      ? 'border-indigo-500 bg-indigo-600 text-signal-ink'
                      : 'border-slate-800 bg-slate-950 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            <input
              type="number"
              min={15}
              max={1440}
              value={intervalMinutes}
              onChange={(e) => setIntervalMinutes(Number(e.target.value))}
              placeholder="Minutos personalizados (15 a 1440)"
              className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white"
            />
          </div>
        </div>

        <div className="flex items-center justify-between border-t border-slate-800 pt-4">
          <label className="inline-flex items-center gap-2 text-sm text-slate-300">
            <input
              type="checkbox"
              checked={autoEnabled}
              onChange={(e) => setAutoEnabled(e.target.checked)}
              className="rounded border-slate-700 accent-indigo-500"
            />
            Monitoramento automático de novas oportunidades ativo
          </label>

          <button
            type="submit"
            disabled={isSaving}
            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-signal-ink hover:bg-indigo-500 disabled:opacity-50"
          >
            <Save className="h-[18px] w-[18px]" />
            {isSaving ? 'Salvando...' : 'Salvar Configurações de Automação'}
          </button>
        </div>
      </form>
    </div>
  );
};
