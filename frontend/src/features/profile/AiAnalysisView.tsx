import React, { useState } from 'react';
import {
  CheckCircle2,
  AlertTriangle,
  Quote,
  Edit3,
  ShieldAlert,
  Sparkles,
} from 'lucide-react';
import type { CandidateProfileDto, ProfileEvidenceDto } from '../../types/api';

interface AiAnalysisViewProps {
  profile: CandidateProfileDto | null;
  onGoToEdit: () => void;
}

function resolveOriginBadge(ev: ProfileEvidenceDto, isProfileConfirmed: boolean) {
  if (isProfileConfirmed) {
    return {
      label: 'Confirmado pelo Usuário',
      className:
        'border-emerald-700/60 bg-emerald-950/60 text-emerald-300',
      icon: CheckCircle2,
    };
  }
  if (!ev.extractedValue || ev.extractedValue.trim() === '') {
    return {
      label: 'Não Encontrado no Documento',
      className: 'border-amber-700/60 bg-amber-950/60 text-amber-300',
      icon: AlertTriangle,
    };
  }
  if (ev.verified) {
    return {
      label: 'Extraído (Evidência Verificada no Currículo)',
      className:
        'border-emerald-700/60 bg-emerald-950/60 text-emerald-300',
      icon: CheckCircle2,
    };
  }
  return {
    label: 'Inferido / Sugerido (Requer Revisão Humana)',
    className: 'border-amber-700/60 bg-amber-950/60 text-amber-300',
    icon: Sparkles,
  };
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
            Todo dado proposto pela IA é confrontado com o texto real do currículo e classificado entre Extraído, Inferido/Sugerido, Não Encontrado ou Confirmado.
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
          Extraídos com Evidência Verificada ({verifiedCount})
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
          Inferidos / Não Encontrados ({reviewCount})
        </button>
      </div>

      {/* Lista de Evidências */}
      <div className="space-y-3">
        {evidences.map((ev) => {
          const badge = resolveOriginBadge(ev, profile.confirmed);
          const BadgeIcon = badge.icon;
          return (
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
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium ${badge.className}`}
                  >
                    <BadgeIcon className="h-3.5 w-3.5" />
                    {badge.label}
                  </span>
                  <button
                    type="button"
                    onClick={onGoToEdit}
                    className="rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-0.5 text-xs text-slate-200 hover:bg-slate-700"
                  >
                    Editar
                  </button>
                </div>
              </div>

              <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
                <div className="rounded-lg border border-slate-800/80 bg-slate-950/60 p-3">
                  <span className="text-[11px] font-medium uppercase tracking-wider text-slate-400">
                    Valor Proposto pela IA
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
          );
        })}
      </div>
    </div>
  );
};
