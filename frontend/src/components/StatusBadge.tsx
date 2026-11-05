import React from 'react';
import type { ResumeStatus } from '../types/api';

const STATUS_CONFIG: Record<
  ResumeStatus,
  { label: string; className: string }
> = {
  UPLOADED: {
    label: 'Enviado',
    className: 'bg-slate-800 text-slate-300 border-slate-700',
  },
  EXTRACTING: {
    label: 'Extraindo texto...',
    className: 'bg-sky-950/70 text-sky-300 border-sky-800',
  },
  EXTRACTED: {
    label: 'Texto extraído',
    className: 'bg-indigo-950/70 text-indigo-300 border-indigo-800',
  },
  ANALYZING: {
    label: 'Analisando com IA...',
    className: 'bg-purple-950/70 text-purple-300 border-purple-800',
  },
  REVIEW_REQUIRED: {
    label: 'Revisão necessária',
    className: 'bg-amber-950/70 text-amber-300 border-amber-800',
  },
  CONFIRMED: {
    label: 'Confirmado',
    className: 'bg-emerald-950/70 text-emerald-300 border-emerald-800',
  },
  FAILED: {
    label: 'Falha no processamento',
    className: 'bg-rose-950/70 text-rose-300 border-rose-800',
  },
};

export const StatusBadge: React.FC<{ status: ResumeStatus }> = ({ status }) => {
  const cfg = STATUS_CONFIG[status] ?? {
    label: status,
    className: 'bg-slate-800 text-slate-300 border-slate-700',
  };
  return (
    <span
      data-testid={`status-badge-${status}`}
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${cfg.className}`}
    >
      {cfg.label}
    </span>
  );
};
