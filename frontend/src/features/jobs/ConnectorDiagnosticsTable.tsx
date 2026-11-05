import React from 'react';
import { Activity, AlertTriangle, CheckCircle2, HelpCircle } from 'lucide-react';
import type { ConnectorDiagnosticDto, SyncOutcomeStatus } from '../../types/api';

interface ConnectorDiagnosticsTableProps {
  diagnostics: ConnectorDiagnosticDto[];
  title?: string;
}

const OUTCOME_BADGES: Record<
  SyncOutcomeStatus,
  { label: string; className: string }
> = {
  SUCCESS_WITH_RESULTS: {
    label: 'Com Vagas',
    className: 'border-emerald-500/40 bg-emerald-950/60 text-emerald-300',
  },
  SUCCESS_EMPTY_CONFIRMED: {
    label: '0 Vagas (Confirmado)',
    className: 'border-slate-600 bg-slate-900 text-slate-300',
  },
  EXTRACTION_INCONCLUSIVE: {
    label: 'Inconclusivo (Preservadas)',
    className: 'border-amber-500/40 bg-amber-950/60 text-amber-300',
  },
  PARSE_ERROR: {
    label: 'Erro de Parsing',
    className: 'border-rose-500/40 bg-rose-950/60 text-rose-300',
  },
  HTTP_ERROR: {
    label: 'Erro HTTP',
    className: 'border-rose-500/40 bg-rose-950/60 text-rose-300',
  },
  NETWORK_ERROR: {
    label: 'Falha de Rede',
    className: 'border-rose-500/40 bg-rose-950/60 text-rose-300',
  },
};

export const ConnectorDiagnosticsTable: React.FC<ConnectorDiagnosticsTableProps> = ({
  diagnostics,
  title = 'Diagnóstico Técnico Reproduzível por Conector / Consulta',
}) => {
  if (!diagnostics || diagnostics.length === 0) {
    return null;
  }

  return (
    <div
      data-testid="connector-diagnostics-table"
      className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div className="flex items-center gap-2">
          <Activity className="h-4 w-4 text-indigo-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
            {title} ({diagnostics.length})
          </h3>
        </div>
        <span className="text-[11px] text-slate-400">
          URL • HTTP • Extraídas • Motivos de Descarte • Salvas/Atualizadas
        </span>
      </div>

      <div className="mt-3 overflow-x-auto">
        <table className="w-full min-w-[720px] text-left text-xs text-slate-300">
          <thead>
            <tr className="border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400">
              <th className="py-2 pr-3">Conector / URL Consultada</th>
              <th className="py-2 px-3">HTTP & Status</th>
              <th className="py-2 px-3 text-center">Extraídas</th>
              <th className="py-2 px-3">Descartadas (Motivos)</th>
              <th className="py-2 pl-3 text-right">Salvas / Atual.</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/70">
            {diagnostics.map((diag, idx) => {
              const badge =
                OUTCOME_BADGES[diag.outcomeStatus] ||
                OUTCOME_BADGES.EXTRACTION_INCONCLUSIVE;
              const reasonsList = Object.entries(diag.discardReasons || {});
              const isError =
                diag.outcomeStatus === 'PARSE_ERROR' ||
                diag.outcomeStatus === 'HTTP_ERROR' ||
                diag.outcomeStatus === 'NETWORK_ERROR';

              return (
                <tr key={`${diag.connectorName}-${idx}`} className="align-top">
                  <td className="py-2.5 pr-3">
                    <div className="font-medium text-white">
                      {diag.connectorName}
                    </div>
                    <div className="mt-0.5 max-w-[300px] truncate font-mono text-[10px] text-slate-400" title={diag.queriedUrl}>
                      {diag.queriedUrl}
                    </div>
                    {diag.errorMessage && (
                      <div className="mt-1 flex items-start gap-1 text-[11px] text-rose-300">
                        <AlertTriangle className="mt-0.5 h-3 w-3 shrink-0" />
                        <span>{diag.errorMessage}</span>
                      </div>
                    )}
                  </td>

                  <td className="py-2.5 px-3">
                    <div className="flex flex-wrap items-center gap-1.5">
                      {diag.httpStatus !== null && diag.httpStatus !== undefined && (
                        <span className="rounded border border-slate-700 bg-slate-950 px-1.5 py-0.5 font-mono text-[10px] text-slate-200">
                          HTTP {diag.httpStatus}
                        </span>
                      )}
                      <span
                        className={`inline-flex items-center gap-1 rounded border px-2 py-0.5 text-[10px] font-medium ${badge.className}`}
                      >
                        {isError ? (
                          <AlertTriangle className="h-3 w-3" />
                        ) : diag.responseContainedJobs ? (
                          <CheckCircle2 className="h-3 w-3" />
                        ) : (
                          <HelpCircle className="h-3 w-3" />
                        )}
                        {badge.label}
                      </span>
                    </div>
                  </td>

                  <td className="py-2.5 px-3 text-center font-semibold text-white">
                    {diag.parsedCount}
                  </td>

                  <td className="py-2.5 px-3">
                    {diag.discardedCount === 0 ? (
                      <span className="text-slate-500">0</span>
                    ) : (
                      <div>
                        <span className="font-medium text-amber-300">
                          {diag.discardedCount} descartada(s)
                        </span>
                        {reasonsList.length > 0 && (
                          <div className="mt-0.5 flex flex-wrap gap-1">
                            {reasonsList.map(([reason, count]) => (
                              <span
                                key={reason}
                                className="rounded border border-slate-700 bg-slate-950 px-1.5 py-0.5 text-[10px] text-slate-300"
                              >
                                {reason}: {count}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </td>

                  <td className="py-2.5 pl-3 text-right">
                    <span className="font-semibold text-emerald-300">
                      +{diag.savedNewCount}
                    </span>
                    <span className="text-slate-500"> / </span>
                    <span className="text-indigo-300">
                      ~{diag.updatedExistingCount}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
