import React, { useState } from 'react';
import {
  Upload,
  FileText,
  Sparkles,
  AlertCircle,
  Loader2,
  Eye,
  CheckCircle2,
  Clock,
} from 'lucide-react';
import type { ResumeDto, ResumeStatus } from '../../types/api';
import { StatusBadge } from '../../components/StatusBadge';

interface ResumeManagerViewProps {
  resumes: ResumeDto[];
  isUploading: boolean;
  analyzingResumeId: string | null;
  onUploadFile: (file: File) => Promise<void>;
  onAnalyzeResume: (id: string) => Promise<void>;
}

const PROCESSING_STEPS: { id: number; label: string; minStage: number }[] = [
  { id: 1, label: '1. Arquivo recebido', minStage: 1 },
  { id: 2, label: '2. Validação concluída', minStage: 1 },
  { id: 3, label: '3. Texto extraído', minStage: 2 },
  { id: 4, label: '4. Análise solicitada à IA', minStage: 3 },
  { id: 5, label: '5. Resultado validado', minStage: 4 },
  { id: 6, label: '6. Perfil aguardando revisão / confirmado', minStage: 4 },
];

function getStageLevel(status: ResumeStatus | null, isUploading: boolean, isAnalyzing: boolean): number {
  if (isUploading) return 1;
  if (!status) return 0;
  if (isAnalyzing || status === 'ANALYZING') return 3;
  switch (status) {
    case 'UPLOADED':
    case 'EXTRACTING':
      return 1;
    case 'EXTRACTED':
      return 2;
    case 'REVIEW_REQUIRED':
      return 4;
    case 'CONFIRMED':
      return 5;
    case 'FAILED':
      return -1;
    default:
      return 0;
  }
}

export const ResumeManagerView: React.FC<ResumeManagerViewProps> = ({
  resumes,
  isUploading,
  analyzingResumeId,
  onUploadFile,
  onAnalyzeResume,
}) => {
  const [selectedResumeId, setSelectedResumeId] = useState<string | null>(
    resumes[0]?.id ?? null
  );
  const [localError, setLocalError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const selectedResume =
    resumes.find((r) => r.id === selectedResumeId) ?? resumes[0] ?? null;

  const processSelectedFile = async (file: File | undefined) => {
    setLocalError(null);
    if (!file) return;

    const lowerName = file.name.toLowerCase();
    if (!lowerName.endsWith('.pdf') && !lowerName.endsWith('.docx')) {
      setLocalError(
        'Selecione apenas arquivos nos formatos PDF (.pdf) ou Word (.docx).'
      );
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setLocalError('O arquivo excede o tamanho máximo permitido de 10 MB.');
      return;
    }

    try {
      await onUploadFile(file);
    } catch (err) {
      setLocalError(
        err instanceof Error ? err.message : 'Falha no envio do arquivo.'
      );
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    try {
      await processSelectedFile(file);
    } finally {
      e.target.value = '';
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLLabelElement>) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isUploading) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent<HTMLLabelElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = async (e: React.DragEvent<HTMLLabelElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    if (isUploading) return;
    const file = e.dataTransfer.files?.[0];
    await processSelectedFile(file);
  };

  const activeStage = getStageLevel(
    selectedResume?.status ?? null,
    isUploading,
    Boolean(selectedResume && analyzingResumeId === selectedResume.id)
  );

  return (
    <div className="space-y-6">
      {/* Tela 1 — Envio do currículo com Drag & Drop real */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
        <h2 className="text-lg font-semibold text-white">
          Importar Currículo (PDF ou DOCX)
        </h2>
        <p className="mt-1 text-sm text-slate-400">
          O documento é validado binariamente, armazenado em diretório privado com identificador interno e seu texto é extraído localmente com Apache PDFBox / Apache POI.
        </p>

        <div className="mt-4">
          <label
            htmlFor="resume-upload-input"
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-8 text-center transition ${
              isDragging
                ? 'border-indigo-400 bg-indigo-950/40'
                : 'border-slate-700 bg-slate-950/60 hover:border-indigo-500/60 hover:bg-slate-900/60'
            }`}
          >
            {isUploading ? (
              <Loader2 className="h-8 w-8 animate-spin text-indigo-400" />
            ) : (
              <Upload className="h-8 w-8 text-indigo-400" />
            )}
            <span className="mt-3 text-sm font-medium text-white">
              {isUploading
                ? 'Enviando e extraindo texto do currículo...'
                : 'Arraste e solte seu arquivo PDF ou DOCX aqui, ou clique para selecionar'}
            </span>
            <span className="mt-1 text-xs text-slate-400">
              Tamanho máximo permitido: 10 MB • Apenas documentos com texto selecionável
            </span>
            <input
              id="resume-upload-input"
              data-testid="resume-upload-input"
              type="file"
              accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              className="hidden"
              disabled={isUploading}
              onChange={handleFileChange}
            />
          </label>
        </div>

        {localError && (
          <div
            role="alert"
            className="mt-4 flex items-center gap-2 rounded-xl border border-rose-800 bg-rose-950/50 px-4 py-3 text-sm text-rose-200"
          >
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
            <span>{localError}</span>
          </div>
        )}
      </div>

      {/* Tela 2 — Etapas Reais do Processamento */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
          Etapas Reais do Processamento (Currículo Selecionado)
        </h3>
        <div className="mt-3 grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-6">
          {PROCESSING_STEPS.map((step) => {
            const done = activeStage >= step.minStage;
            return (
              <div
                key={step.id}
                className={`flex items-center gap-2 rounded-xl border px-3 py-2.5 text-xs ${
                  done
                    ? 'border-emerald-700/60 bg-emerald-950/30 text-emerald-200'
                    : 'border-slate-800 bg-slate-950/60 text-slate-400'
                }`}
              >
                {done ? (
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
                ) : (
                  <Clock className="h-4 w-4 shrink-0 text-slate-500" />
                )}
                <span className="font-medium">{step.label}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Lista de currículos e visualização do texto extraído */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="space-y-3 lg:col-span-7">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
            Histórico de Currículos ({resumes.length})
          </h3>
          {resumes.length === 0 ? (
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-8 text-center text-sm text-slate-400">
              Nenhum currículo importado até o momento.
            </div>
          ) : (
            resumes.map((resume) => {
              const isAnalyzing = analyzingResumeId === resume.id;
              return (
                <div
                  key={resume.id}
                  className={`rounded-xl border p-4 transition ${
                    selectedResume?.id === resume.id
                      ? 'border-indigo-500/70 bg-slate-900'
                      : 'border-slate-800 bg-slate-900/50 hover:border-slate-700'
                  }`}
                >
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-400">
                        <FileText className="h-5 w-5" />
                      </div>
                      <div>
                        <p className="font-medium text-white">
                          {resume.filename}
                        </p>
                        <p className="text-xs text-slate-400">
                          {resume.fileType} • {(resume.fileSizeBytes / 1024).toFixed(1)} KB •{' '}
                          {resume.extractedTextLength} caracteres extraídos
                        </p>
                      </div>
                    </div>
                    <StatusBadge status={resume.status} />
                  </div>

                  {resume.errorMessage && (
                    <div className="mt-3 rounded-lg border border-rose-800/60 bg-rose-950/40 px-3 py-2 text-xs text-rose-300">
                      {resume.errorMessage}
                    </div>
                  )}

                  <div className="mt-4 flex flex-wrap items-center justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setSelectedResumeId(resume.id)}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-1.5 text-xs font-medium text-slate-200 hover:bg-slate-800"
                    >
                      <Eye className="h-3.5 w-3.5" />
                      Ver texto extraído
                    </button>
                    {resume.status !== 'FAILED' && (
                      <button
                        type="button"
                        data-testid={`analyze-btn-${resume.id}`}
                        disabled={isAnalyzing}
                        onClick={() => void onAnalyzeResume(resume.id)}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3.5 py-1.5 text-xs font-medium text-white transition hover:bg-indigo-500 disabled:opacity-50"
                      >
                        {isAnalyzing ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <Sparkles className="h-3.5 w-3.5" />
                        )}
                        {isAnalyzing
                          ? 'Analisando...'
                          : 'Estruturar Perfil com IA'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="lg:col-span-5">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
            Inspeção do Texto Extraído
          </h3>
          <div className="mt-3 rounded-xl border border-slate-800 bg-slate-900/60 p-4">
            {selectedResume ? (
              <>
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <span className="text-xs font-medium text-slate-300">
                    {selectedResume.filename}
                  </span>
                  <span className="text-xs text-slate-400">
                    {selectedResume.extractedTextLength} chars
                  </span>
                </div>
                <pre className="mt-3 max-h-96 overflow-y-auto whitespace-pre-wrap font-mono text-xs leading-relaxed text-slate-300">
                  {selectedResume.extractedTextPreview ||
                    'Nenhum texto extraído disponível para este arquivo.'}
                </pre>
              </>
            ) : (
              <p className="text-xs text-slate-400">
                Selecione um currículo ao lado para inspecionar o texto extraído.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
