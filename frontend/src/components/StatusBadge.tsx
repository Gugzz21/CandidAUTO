import React from 'react';
import type { ResumeStatus } from '../types/api';

const STATUS_CONFIG: Record<
  ResumeStatus,
  { label: string; className: string; dot: string; live?: boolean }
> = {
  UPLOADED: {
    label: 'Enviado',
    className: 'border-slate-700 text-slate-300',
    dot: 'bg-slate-400',
  },
  EXTRACTING: {
    label: 'Extraindo texto',
    className: 'border-sky-800 text-sky-300',
    dot: 'bg-sky-400',
    live: true,
  },
  EXTRACTED: {
    label: 'Texto extraído',
    className: 'border-slate-700 text-slate-200',
    dot: 'bg-sky-400',
  },
  ANALYZING: {
    label: 'IA analisando',
    className: 'border-indigo-800 text-indigo-300',
    dot: 'bg-indigo-400',
    live: true,
  },
  REVIEW_REQUIRED: {
    label: 'Revisão necessária',
    className: 'border-amber-800 text-amber-300',
    dot: 'bg-amber-400',
  },
  CONFIRMED: {
    label: 'Confirmado',
    className: 'border-emerald-800 text-emerald-300',
    dot: 'bg-emerald-400',
  },
  FAILED: {
    label: 'Falha no processamento',
    className: 'border-rose-800 text-rose-300',
    dot: 'bg-rose-400',
  },
};

export const StatusBadge: React.FC<{ status: ResumeStatus }> = ({ status }) => {
  const cfg = STATUS_CONFIG[status] ?? {
    label: status,
    className: 'border-slate-700 text-slate-300',
    dot: 'bg-slate-400',
  };
  return (
    <span
      data-testid={`status-badge-${status}`}
      className={`inline-flex items-center gap-1.5 rounded-md border bg-slate-950/60 px-2 py-0.5 font-mono text-xs font-medium uppercase tracking-[0.06em] ${cfg.className}`}
    >
      <span
        aria-hidden="true"
        className={`h-1.5 w-1.5 rounded-full ${cfg.dot} ${cfg.live ? 'animate-live' : ''}`}
      />
      {cfg.label}
    </span>
  );
};
