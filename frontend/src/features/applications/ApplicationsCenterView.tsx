import React, { useEffect, useState } from 'react';
import {
  Send,
  XCircle,
  CheckCircle2,
  AlertTriangle,
  History,
  ExternalLink,
  ShieldCheck,
  KeyRound,
  Save,
} from 'lucide-react';
import type {
  AnswerMissingQuestionsRequestDto,
  ApplicationDto,
  ApplicationStatus,
  CancelApplicationRequestDto,
  QaVaultItemDto,
  SubmitApplicationRequestDto,
  UpdateApplicationStatusRequestDto,
} from '../../types/api';

interface ApplicationsCenterViewProps {
  applications: ApplicationDto[];
  qaVault: QaVaultItemDto[];
  selectedApplicationId: string | null;
  onSelectApplication: (id: string | null) => void;
  onAnswerMissingQuestions: (
    applicationId: string,
    payload: AnswerMissingQuestionsRequestDto
  ) => Promise<void>;
  onSubmitApplication: (
    applicationId: string,
    payload: SubmitApplicationRequestDto
  ) => Promise<void>;
  onCancelApplication: (
    applicationId: string,
    payload: CancelApplicationRequestDto
  ) => Promise<void>;
  onUpdateStatus: (
    applicationId: string,
    payload: UpdateApplicationStatusRequestDto
  ) => Promise<void>;
}

const ALL_STATUSES: { value: ApplicationStatus; label: string }[] = [
  { value: 'ENCONTRADA', label: 'Encontrada' },
  { value: 'EM_ANALISE_IA', label: 'Em análise pela IA' },
  { value: 'AGUARDANDO_REVISAO', label: 'Aguardando revisão' },
  { value: 'PRONTA_PARA_CANDIDATURA', label: 'Pronta para candidatura' },
  { value: 'CANDIDATURA_ENVIADA', label: 'Candidatura enviada' },
  { value: 'EM_ANDAMENTO', label: 'Em andamento' },
  { value: 'ENTREVISTA_AGENDADA', label: 'Entrevista agendada' },
  { value: 'APROVADO', label: 'Aprovado' },
  { value: 'REPROVADO', label: 'Reprovado' },
  { value: 'CANCELADA', label: 'Cancelada' },
  { value: 'VAGA_ENCERRADA', label: 'Vaga encerrada' },
];

export const ApplicationsCenterView: React.FC<ApplicationsCenterViewProps> = ({
  applications,
  qaVault,
  selectedApplicationId,
  onSelectApplication,
  onAnswerMissingQuestions,
  onSubmitApplication,
  onCancelApplication,
  onUpdateStatus,
}) => {
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | ''>('');
  const [answersForm, setAnswersForm] = useState<Record<string, string>>({});
  const [saveToVault, setSaveToVault] = useState(true);
  const [submissionNotes, setSubmissionNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedApp =
    applications.find((a) => a.id === selectedApplicationId) ??
    applications[0] ??
    null;

  useEffect(() => {
    if (selectedApp) {
      const initial: Record<string, string> = {};
      for (const mq of selectedApp.missingQuestions) {
        initial[mq.questionKey] = '';
      }
      setAnswersForm(initial);
      setSubmissionNotes(selectedApp.notes ?? '');
    }
  }, [selectedApp?.id]);

  const filteredApps = applications.filter(
    (app) => !statusFilter || app.status === statusFilter
  );

  const handleSaveMissingAnswers = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedApp) return;
    setIsSubmitting(true);
    try {
      await onAnswerMissingQuestions(selectedApp.id, {
        answers: answersForm,
        saveToVault,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleExplicitSubmit = async () => {
    if (!selectedApp) return;
    setIsSubmitting(true);
    try {
      await onSubmitApplication(selectedApp.id, {
        explicitApprovalConfirmed: true,
        notes: submissionNotes,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = async (keepAsDraft: boolean) => {
    if (!selectedApp) return;
    setIsSubmitting(true);
    try {
      await onCancelApplication(selectedApp.id, {
        keepAsDraft,
        reason: keepAsDraft
          ? 'Preenchimento mantido como rascunho pelo candidato na revisão.'
          : 'Candidatura descartada pelo candidato na tela de revisão.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-300">
          <ShieldCheck className="h-3.5 w-3.5" />
          Etapas 4, 5 & 6 • Preenchimento Inteligente, Revisão Obrigatória e Acompanhamento
        </span>
        <h1 className="mt-2 text-xl font-semibold text-white">
          Central de Candidaturas e Revisão Final
        </h1>
        <p className="mt-1 text-sm text-slate-400">
          Nenhuma candidatura é enviada sem sua aprovação explícita. A IA utiliza exclusivamente dados verdadeiros e confirmados do seu currículo e cofre de respostas.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Coluna Esquerda: Lista de Candidaturas + Cofre QA */}
        <div className="space-y-6 lg:col-span-5">
          <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
            <div className="mb-4 flex items-center justify-between gap-2">
              <h2 className="text-sm font-semibold text-white">
                Histórico de Candidaturas ({filteredApps.length})
              </h2>
              <select
                value={statusFilter}
                onChange={(e) =>
                  setStatusFilter(e.target.value as ApplicationStatus | '')
                }
                className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-xs text-white"
              >
                <option value="">Todos os 11 status</option>
                {ALL_STATUSES.map((st) => (
                  <option key={st.value} value={st.value}>
                    {st.label}
                  </option>
                ))}
              </select>
            </div>

            {filteredApps.length === 0 ? (
              <p className="py-6 text-center text-xs text-slate-400">
                Nenhuma candidatura em preparação ou enviada. No "Painel de Oportunidades", clique em "Preparar Candidatura com IA".
              </p>
            ) : (
              <div className="space-y-2.5">
                {filteredApps.map((app) => {
                  const isSelected = selectedApp?.id === app.id;
                  const statusLabel =
                    ALL_STATUSES.find((s) => s.value === app.status)?.label ??
                    app.status;
                  return (
                    <button
                      key={app.id}
                      type="button"
                      onClick={() => onSelectApplication(app.id)}
                      className={`w-full rounded-xl border p-3.5 text-left transition ${
                        isSelected
                          ? 'border-indigo-500 bg-indigo-950/30'
                          : 'border-slate-800 bg-slate-950/60 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-semibold text-white">
                          {app.job.title}
                        </span>
                        {app.matchAnalysis && (
                          <span className="rounded-md bg-indigo-500/20 px-2 py-0.5 text-[11px] font-bold text-indigo-300">
                            {app.matchAnalysis.compatibilityScore}%
                          </span>
                        )}
                      </div>
                      <p className="mt-0.5 text-xs text-slate-400">
                        {app.job.companyName} • {app.job.workModality}
                      </p>
                      <div className="mt-2 flex items-center justify-between">
                        <span className="rounded-md border border-slate-700 bg-slate-900 px-2 py-0.5 text-[11px] text-slate-300">
                          {statusLabel}
                        </span>
                        <span className="text-[11px] text-slate-500">
                          {app.submissionMode === 'INTEGRACAO_AUTORIZADA'
                            ? 'Integração Autorizada'
                            : 'Fluxo Assistido'}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Cofre de Respostas Confirmadas do Candidato */}
          <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
            <h2 className="flex items-center gap-2 text-sm font-semibold text-white">
              <KeyRound className="h-4 w-4 text-emerald-400" />
              Cofre de Respostas Confirmadas ({qaVault.length})
            </h2>
            <p className="mt-1 text-xs text-slate-400">
              Respostas reais fornecidas por você e reutilizadas com segurança em formulários futuros.
            </p>
            {qaVault.length === 0 ? (
              <p className="mt-3 text-xs text-slate-500">
                Nenhuma resposta adicional salva ainda.
              </p>
            ) : (
              <div className="mt-3 space-y-2">
                {qaVault.map((item) => (
                  <div
                    key={item.id}
                    className="rounded-lg border border-slate-800 bg-slate-950/70 p-2.5 text-xs"
                  >
                    <p className="font-medium text-slate-300">
                      {item.questionLabel}
                    </p>
                    <p className="mt-0.5 text-emerald-300">
                      Resposta: {item.confirmedAnswer}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Coluna Direita: Tela de Revisão, Preenchimento Inteligente e Botões CANDIDATAR-SE / CANCELAR */}
        <div className="lg:col-span-7">
          {!selectedApp ? (
            <div className="rounded-2xl border border-slate-800 bg-slate-900/50 p-10 text-center">
              <p className="text-sm text-slate-400">
                Selecione uma candidatura ao lado ou prepare uma nova no Painel de Oportunidades para revisar campos e confirmar o envio.
              </p>
            </div>
          ) : (
            <div className="space-y-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
              {/* Cabeçalho da Candidatura Selecionada */}
              <div className="flex flex-col justify-between gap-4 border-b border-slate-800 pb-4 sm:flex-row sm:items-center">
                <div>
                  <span className="text-xs font-medium text-indigo-400">
                    {selectedApp.job.companyName} •{' '}
                    {selectedApp.job.sourcePlatform}
                  </span>
                  <h2 className="text-lg font-semibold text-white">
                    {selectedApp.job.title}
                  </h2>
                  <p className="text-xs text-slate-400">
                    Modo de Candidatura:{' '}
                    <strong className="text-slate-200">
                      {selectedApp.submissionMode === 'INTEGRACAO_AUTORIZADA'
                        ? 'Submissão via Integração Autorizada'
                        : 'Fluxo Assistido com Dados Preparados'}
                    </strong>
                  </p>
                </div>

                <a
                  href={selectedApp.job.originalUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-700"
                >
                  Abrir Link Oficial da Vaga <ExternalLink className="h-3.5 w-3.5" />
                </a>
              </div>

              {/* 1. Campos Preenchidos Automaticamente com Dados Verdadeiros */}
              <div>
                <h3 className="flex items-center gap-2 text-sm font-semibold text-white">
                  <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                  Campos Preparados pela IA (Baseados no Currículo Confirmado)
                </h3>
                <div className="mt-3 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                  {selectedApp.preparedFields.map((field) => (
                    <div
                      key={field.fieldName}
                      className={`rounded-xl border p-3 text-xs ${
                        field.missing
                          ? 'border-amber-700/70 bg-amber-950/30'
                          : 'border-slate-800 bg-slate-950/70'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-200">
                          {field.fieldLabel}
                        </span>
                        {field.required && (
                          <span className="text-[10px] text-amber-400">
                            Obrigatório
                          </span>
                        )}
                      </div>
                      <p className="mt-1 break-words text-slate-100">
                        {field.value || (
                          <em className="text-amber-300">
                            Aguardando confirmação manual abaixo
                          </em>
                        )}
                      </p>
                      <p className="mt-1 text-[10px] text-slate-500">
                        Origem: {field.sourceEvidence}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* 2. Perguntas Ausentes que Exigem Confirmação do Usuário */}
              {selectedApp.missingQuestions.length > 0 && (
                <form
                  onSubmit={handleSaveMissingAnswers}
                  className="space-y-4 rounded-xl border border-amber-700/60 bg-amber-950/20 p-4"
                >
                  <div className="flex items-start gap-2">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                    <div>
                      <h4 className="text-xs font-semibold text-amber-200">
                        Informações Adicionais Solicitadas pela Empresa (
                        {selectedApp.missingQuestions.length})
                      </h4>
                      <p className="text-xs text-amber-300/80">
                        Para respeitar a regra de zero alucinação, responda abaixo com informações verdadeiras antes de aprovar o envio:
                      </p>
                    </div>
                  </div>

                  {selectedApp.missingQuestions.map((mq) => (
                    <div key={mq.questionKey} className="space-y-1">
                      <label className="block text-xs font-medium text-white">
                        {mq.questionLabel}{' '}
                        {mq.required && (
                          <span className="text-amber-400">*</span>
                        )}
                      </label>
                      <input
                        type="text"
                        required={mq.required}
                        value={answersForm[mq.questionKey] ?? ''}
                        onChange={(e) =>
                          setAnswersForm((prev) => ({
                            ...prev,
                            [mq.questionKey]: e.target.value,
                          }))
                        }
                        placeholder="Digite sua resposta verdadeira..."
                        className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
                      />
                    </div>
                  ))}

                  <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                    <label className="inline-flex items-center gap-2 text-xs text-slate-300">
                      <input
                        type="checkbox"
                        checked={saveToVault}
                        onChange={(e) => setSaveToVault(e.target.checked)}
                        className="rounded border-slate-700 accent-indigo-500"
                      />
                      Salvar respostas no Cofre do Candidato para próximas vagas
                    </label>

                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-500 disabled:opacity-50"
                    >
                      <Save className="h-3.5 w-3.5" />
                      Confirmar Respostas e Atualizar Revisão
                    </button>
                  </div>
                </form>
              )}

              {/* 3. Etapa 5: Tela de Revisão e Aprovação Explícita (BOTÃO CANDIDATAR-SE / BOTÃO CANCELAR) */}
              <div className="space-y-4 rounded-xl border border-indigo-500/40 bg-indigo-950/20 p-5">
                <h3 className="text-sm font-semibold text-white">
                  Decisão Final e Aprovação Explícita do Candidato
                </h3>
                <p className="text-xs text-slate-300">
                  Revise todos os campos preparados acima. Ao clicar em{' '}
                  <strong>CANDIDATAR-SE</strong>, você autoriza explicitamente o registro e envio da candidatura. Ao clicar em{' '}
                  <strong>CANCELAR</strong>, nenhum dado será enviado à empresa.
                </p>

                <div>
                  <label className="block text-xs font-medium text-slate-300">
                    Observações / Notas da Candidatura (opcional)
                  </label>
                  <input
                    type="text"
                    value={submissionNotes}
                    onChange={(e) => setSubmissionNotes(e.target.value)}
                    placeholder="Ex: Currículo adaptado para vaga Full Stack..."
                    className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-white"
                  />
                </div>

                {selectedApp.submissionReceipt && (
                  <div className="rounded-lg border border-emerald-700/60 bg-emerald-950/40 p-3 text-xs text-emerald-200">
                    <strong>Comprovante Registrado:</strong>{' '}
                    {selectedApp.submissionReceipt}
                  </div>
                )}

                <div className="flex flex-wrap items-center gap-3 pt-2">
                  <button
                    type="button"
                    data-testid="btn-candidatar-se"
                    disabled={
                      isSubmitting ||
                      selectedApp.missingQuestions.some((q) => q.required)
                    }
                    onClick={() => void handleExplicitSubmit()}
                    className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs font-bold uppercase tracking-wider text-white shadow-sm transition hover:bg-emerald-500 disabled:opacity-50"
                  >
                    <Send className="h-4 w-4" />
                    CANDIDATAR-SE
                  </button>

                  <button
                    type="button"
                    data-testid="btn-cancelar-rascunho"
                    disabled={isSubmitting}
                    onClick={() => void handleCancel(true)}
                    className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-xs font-semibold text-slate-200 transition hover:bg-slate-700"
                  >
                    <XCircle className="h-4 w-4 text-amber-400" />
                    CANCELAR (Manter Rascunho)
                  </button>

                  <button
                    type="button"
                    data-testid="btn-cancelar-descartar"
                    disabled={isSubmitting}
                    onClick={() => void handleCancel(false)}
                    className="inline-flex items-center gap-2 rounded-xl border border-rose-800/70 bg-rose-950/50 px-4 py-2.5 text-xs font-semibold text-rose-200 transition hover:bg-rose-900/60"
                  >
                    <XCircle className="h-4 w-4 text-rose-400" />
                    CANCELAR (Descartar)
                  </button>
                </div>
              </div>

              {/* 4. Etapa 6: Atualização Manual do Status do Processo Seletivo */}
              <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                <label className="block text-xs font-semibold text-white">
                  Atualizar Status de Acompanhamento do Processo Seletivo:
                </label>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  {ALL_STATUSES.map((st) => (
                    <button
                      key={st.value}
                      type="button"
                      disabled={
                        isSubmitting ||
                        (st.value === 'CANDIDATURA_ENVIADA' &&
                          !selectedApp.userApprovedExplicitly)
                      }
                      onClick={() =>
                        void onUpdateStatus(selectedApp.id, {
                          status: st.value,
                          notes: submissionNotes,
                        })
                      }
                      className={`rounded-lg border px-2.5 py-1 text-[11px] font-medium transition ${
                        selectedApp.status === st.value
                          ? 'border-indigo-500 bg-indigo-600 text-white'
                          : 'border-slate-800 bg-slate-900 text-slate-300 hover:border-slate-700'
                      } disabled:opacity-40`}
                    >
                      {st.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* 5. Log de Auditoria */}
              <div>
                <h4 className="flex items-center gap-1.5 text-xs font-semibold text-slate-300">
                  <History className="h-3.5 w-3.5 text-indigo-400" />
                  Histórico Auditável da Candidatura
                </h4>
                <div className="mt-2 space-y-1.5">
                  {selectedApp.auditLog.map((ev, idx) => (
                    <div
                      key={idx}
                      className="rounded-lg border border-slate-800/80 bg-slate-950/70 px-3 py-2 text-[11px] text-slate-400"
                    >
                      <span className="font-semibold text-indigo-300">
                        [{ev.status}]
                      </span>{' '}
                      <span className="text-slate-200">{ev.details}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
