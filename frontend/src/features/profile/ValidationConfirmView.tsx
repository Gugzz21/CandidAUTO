import React, { useState } from 'react';
import {
  CheckCircle2,
  AlertTriangle,
  ShieldCheck,
  Loader2,
  Edit3,
} from 'lucide-react';
import type { CandidateProfileDto } from '../../types/api';

interface ValidationConfirmViewProps {
  profile: CandidateProfileDto | null;
  isConfirming: boolean;
  onConfirmProfile: () => Promise<void>;
  onGoToEdit: () => void;
}

export const ValidationConfirmView: React.FC<ValidationConfirmViewProps> = ({
  profile,
  isConfirming,
  onConfirmProfile,
  onGoToEdit,
}) => {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!profile || !profile.fullName) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-8 text-center">
        <AlertTriangle className="mx-auto h-10 w-10 text-amber-400" />
        <h2 className="mt-3 text-xl font-semibold text-white">
          Perfil incompleto para validação
        </h2>
        <p className="mt-1 text-base text-slate-400">
          Importe seu currículo ou preencha seu nome completo na aba "Perfil" antes de confirmar os dados.
        </p>
        <button
          type="button"
          onClick={onGoToEdit}
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-base font-medium text-signal-ink hover:bg-indigo-500"
        >
          <Edit3 className="h-[18px] w-[18px]" />
          Preencher Perfil
        </button>
      </div>
    );
  }

  const pendingReviews = profile.evidences.filter((e) => e.needsReview);

  const handleConfirm = async () => {
    setErrorMsg(null);
    try {
      await onConfirmProfile();
    } catch (err) {
      setErrorMsg(
        err instanceof Error ? err.message : 'Erro ao confirmar perfil.'
      );
    }
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <span className="eyebrow inline-flex items-center gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/10 px-2 py-1 text-emerald-300">
              <ShieldCheck className="h-4 w-4" />
              Aprovação Explícita do Candidato
            </span>
            <h2 className="mt-2 text-xl font-semibold text-white">
              Revisão Final e Confirmação do Perfil
            </h2>
            <p className="mt-1 text-base text-slate-400">
              Ao confirmar, você atesta que os dados abaixo são verdadeiros e autoriza sua utilização na etapa futura de análise de compatibilidade e preenchimento assistido.
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <button
              type="button"
              onClick={onGoToEdit}
              className="rounded-xl border border-slate-700 bg-slate-850 px-4 py-2.5 text-base font-medium text-slate-200 hover:bg-slate-700"
            >
              Voltar e Editar
            </button>
            <button
              type="button"
              data-testid="confirm-profile-btn"
              disabled={isConfirming || profile.confirmed}
              onClick={handleConfirm}
              className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-base font-semibold text-signal-ink transition hover:bg-indigo-500 disabled:opacity-60"
            >
              {isConfirming ? (
                <Loader2 className="h-[18px] w-[18px] animate-spin" />
              ) : (
                <CheckCircle2 className="h-[18px] w-[18px]" />
              )}
              {profile.confirmed
                ? 'Perfil Confirmado'
                : 'Confirmar Perfil Profissional'}
            </button>
          </div>
        </div>

        {profile.confirmed && (
          <div
            data-testid="profile-confirmed-banner"
            className="mt-4 flex items-center gap-2 rounded-xl border border-emerald-700/70 bg-emerald-950/50 px-4 py-3 text-base text-emerald-200"
          >
            <CheckCircle2 className="h-[18px] w-[18px] shrink-0 text-emerald-400" />
            <span>
              Perfil profissional confirmado e validado com sucesso!
              {profile.confirmedAt
                ? ` (${new Date(profile.confirmedAt).toLocaleString('pt-BR')})`
                : ''}
            </span>
          </div>
        )}

        {errorMsg && (
          <div
            role="alert"
            className="mt-4 rounded-xl border border-rose-800 bg-rose-950/50 px-4 py-3 text-base text-rose-200"
          >
            {errorMsg}
          </div>
        )}
      </div>

      {pendingReviews.length > 0 && !profile.confirmed && (
        <div className="rounded-xl border border-amber-700/60 bg-amber-950/30 p-4">
          <div className="flex items-center gap-2 text-base font-semibold text-amber-300">
            <AlertTriangle className="h-[18px] w-[18px]" />
            <span>
              Existem {pendingReviews.length} campo(s) marcados para atenção antes da confirmação:
            </span>
          </div>
          <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-amber-200/90">
            {pendingReviews.map((item) => (
              <li key={item.id}>
                <strong>{item.fieldLabel}:</strong> {item.evidenceText}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Resumo consolidado do Perfil */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h3 className="eyebrow text-slate-400">
            Dados do Candidato
          </h3>
          <p className="mt-2 text-2xl font-bold text-white">{profile.fullName}</p>
          <p className="text-base text-indigo-400">
            {profile.headline || 'Título não informado'} •{' '}
            {profile.seniorityLevel || 'Senioridade não informada'}
          </p>
          <p className="mt-3 text-sm leading-relaxed text-slate-300">
            {profile.summary || 'Sem resumo cadastrado.'}
          </p>
          <div className="mt-4 space-y-1 border-t border-slate-800 pt-3 text-sm text-slate-400">
            <p>E-mail: {profile.contactInfo?.email || '—'}</p>
            <p>Telefone: {profile.contactInfo?.phone || '—'}</p>
            <p>Localização: {profile.contactInfo?.location || '—'}</p>
            <p>LinkedIn: {profile.contactInfo?.linkedinUrl || '—'}</p>
            <p>GitHub: {profile.contactInfo?.githubUrl || '—'}</p>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h3 className="eyebrow text-slate-400">
            Competências Confirmadas ({profile.skills.length})
          </h3>
          <div className="mt-3 flex flex-wrap gap-1.5">
            {profile.skills.length === 0 ? (
              <span className="text-sm text-slate-400">
                Nenhuma competência listada.
              </span>
            ) : (
              profile.skills.map((s, idx) => (
                <span
                  key={`${s.name}-${idx}`}
                  className="rounded-md border border-indigo-500/30 bg-indigo-500/10 px-2.5 py-1 text-sm font-medium text-indigo-200"
                >
                  {s.name}
                </span>
              ))
            )}
          </div>

          <h3 className="mt-6 eyebrow text-slate-400">
            Cargos Alvo ({profile.targetRoles.length})
          </h3>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {profile.targetRoles.length === 0 ? (
              <span className="text-sm text-slate-400">
                Nenhum cargo alvo definido.
              </span>
            ) : (
              profile.targetRoles.map((role, idx) => (
                <span
                  key={idx}
                  className="rounded-md border border-slate-700 bg-slate-850 px-2.5 py-1 text-sm text-slate-200"
                >
                  {role}
                </span>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
