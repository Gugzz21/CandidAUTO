import React, { useState } from 'react';
import {
  CheckCircle2,
  AlertTriangle,
  Quote,
  Edit3,
  ShieldAlert,
} from 'lucide-react';
import type { CandidateProfileDto } from '../../types/api';

interface AiAnalysisViewProps {
  profile: CandidateProfileDto | null;
  onGoToEdit: () => void;
}

export const AiAnalysisView: React.FC<AiAnalysisViewProps> = ({
  profile,
  onGoToEdit,
}) => {
  const [filter, setFilter] = useState<'ALL' | 'VERIFIED' | 'REVIEW'>('ALL');

  if (!profile || (!profile.fullName && profile.evidences.length === 0)) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-8 text-center">
        <ShieldAlert className="mx-auto h-10 w-10 text-indigo-400" />
        <h2 className="mt-3 text-lg font-semibold text-white">
          Nenhuma análise de IA executada ainda
        </h2>
        <p className="mt-1 text-sm text-slate-400">
          Importe um currículo na aba "Currículo" e clique em "Estruturar Perfil com IA" para rastrear as evidências textuais.
        </p>
      </div>
    );
  }

  const evidences = profile.evidences.filter((ev) => {
    if (filter === 'VERIFIED') return ev.verified;
    if (filter === 'REVIEW') return ev.needsReview;
    return true;
  });

  const verifiedCount = profile.evidences.filter((e) => e.verified).length;
  const reviewCount = profile.evidences.filter((e) => e.needsReview).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold text-white">
            Auditoria de Evidências da IA (Zero-Alucinação)
          </h2>
          <p className="mt-1 text-sm text-slate-400">
            Todo dado extraído pela IA é confrontado com o texto real do currículo. Campos ausentes ou sem trecho literal correspondente são destacados para sua revisão.
          </p>
        </div>
        <button
          type="button"
          onClick={onGoToEdit}
          className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-indigo-500"
        >
          <Edit3 className="h-4 w-4" />
          Revisar e Editar Perfil
        </button>
      </div>

      {/* Filtros de Evidências */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => setFilter('ALL')}
          className={`rounded-lg px-3.5 py-1.5 text-xs font-medium transition ${
            filter === 'ALL'
              ? 'bg-indigo-600 text-white'
              : 'border border-slate-800 bg-slate-900 text-slate-300 hover:bg-slate-800'
          }`}
        >
          Todos os Campos ({profile.evidences.length})
        </button>
        <button
          type="button"
          onClick={() => setFilter('VERIFIED')}
          className={`rounded-lg px-3.5 py-1.5 text-xs font-medium transition ${
            filter === 'VERIFIED'
              ? 'bg-emerald-600 text-white'
              : 'border border-slate-800 bg-slate-900 text-emerald-300 hover:bg-slate-800'
          }`}
        >
          Com Evidência Verificada ({verifiedCount})
        </button>
        <button
          type="button"
          onClick={() => setFilter('REVIEW')}
          className={`rounded-lg px-3.5 py-1.5 text-xs font-medium transition ${
            filter === 'REVIEW'
              ? 'bg-amber-600 text-white'
              : 'border border-slate-800 bg-slate-900 text-amber-300 hover:bg-slate-800'
          }`}
        >
          Atenção / Revisão Necessária ({reviewCount})
        </button>
      </div>

      {/* Lista de Evidências */}
      <div className="space-y-3">
        {evidences.map((ev) => (
          <div
            key={ev.id}
            data-testid={`evidence-item-${ev.fieldPath}`}
            className={`rounded-xl border p-4 ${
              ev.needsReview
                ? 'border-amber-700/60 bg-amber-950/20'
                : 'border-slate-800 bg-slate-900/60'
            }`}
          >
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <span className="text-xs font-mono text-slate-400">
                  {ev.fieldPath}
                </span>
                <h3 className="text-sm font-semibold text-white">
                  {ev.fieldLabel}
                </h3>
              </div>
              {ev.verified ? (
                <span className="inline-flex items-center gap-1 rounded-full border border-emerald-700/60 bg-emerald-950/60 px-2.5 py-0.5 text-xs font-medium text-emerald-300">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Evidência Verificada no Currículo
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 rounded-full border border-amber-700/60 bg-amber-950/60 px-2.5 py-0.5 text-xs font-medium text-amber-300">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  Requer Revisão Humana
                </span>
              )}
            </div>

            <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="rounded-lg border border-slate-800/80 bg-slate-950/60 p-3">
                <span className="text-[11px] font-medium uppercase tracking-wider text-slate-400">
                  Valor Proposto
                </span>
                <p className="mt-1 text-sm font-medium text-slate-100">
                  {ev.extractedValue || (
                    <span className="italic text-amber-300">
                      Não identificado no currículo
                    </span>
                  )}
                </p>
              </div>
              <div className="rounded-lg border border-slate-800/80 bg-slate-950/60 p-3">
                <span className="inline-flex items-center gap-1 text-[11px] font-medium uppercase tracking-wider text-slate-400">
                  <Quote className="h-3 w-3" /> Trecho de Evidência / Observação
                </span>
                <p className="mt-1 text-xs text-slate-300">
                  {ev.evidenceText}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
