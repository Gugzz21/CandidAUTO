import React, { useEffect, useState } from 'react';
import {
  Save,
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Loader2,
} from 'lucide-react';
import type {
  CandidateProfileDto,
  CertificationItemDto,
  EducationItemDto,
  ExperienceItemDto,
  LanguageItemDto,
  ProjectItemDto,
  SkillItemDto,
  UpdateProfileRequestDto,
} from '../../types/api';

interface ProfileEditorViewProps {
  profile: CandidateProfileDto | null;
  isSaving: boolean;
  onSaveProfile: (payload: UpdateProfileRequestDto) => Promise<void>;
}

export const ProfileEditorView: React.FC<ProfileEditorViewProps> = ({
  profile,
  isSaving,
  onSaveProfile,
}) => {
  const [fullName, setFullName] = useState('');
  const [headline, setHeadline] = useState('');
  const [seniorityLevel, setSeniorityLevel] = useState('');
  const [summary, setSummary] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [location, setLocation] = useState('');
  const [linkedinUrl, setLinkedinUrl] = useState('');
  const [githubUrl, setGithubUrl] = useState('');
  const [portfolioUrl, setPortfolioUrl] = useState('');
  const [targetRolesInput, setTargetRolesInput] = useState('');

  const [skills, setSkills] = useState<SkillItemDto[]>([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillCategory, setNewSkillCategory] = useState('LANGUAGE_TECH');

  const [experiences, setExperiences] = useState<ExperienceItemDto[]>([]);
  const [education, setEducation] = useState<EducationItemDto[]>([]);
  const [languages, setLanguages] = useState<LanguageItemDto[]>([]);
  const [certifications, setCertifications] = useState<CertificationItemDto[]>([]);
  const [projects, setProjects] = useState<ProjectItemDto[]>([]);

  const [feedback, setFeedback] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  useEffect(() => {
    if (!profile) return;
    setFullName(profile.fullName ?? '');
    setHeadline(profile.headline ?? '');
    setSeniorityLevel(profile.seniorityLevel ?? '');
    setSummary(profile.summary ?? '');
    setEmail(profile.contactInfo?.email ?? '');
    setPhone(profile.contactInfo?.phone ?? '');
    setLocation(profile.contactInfo?.location ?? '');
    setLinkedinUrl(profile.contactInfo?.linkedinUrl ?? '');
    setGithubUrl(profile.contactInfo?.githubUrl ?? '');
    setPortfolioUrl(profile.contactInfo?.portfolioUrl ?? '');
    setTargetRolesInput((profile.targetRoles ?? []).join(', '));
    setSkills(profile.skills ?? []);
    setExperiences(profile.experiences ?? []);
    setEducation(profile.education ?? []);
    setLanguages(profile.languages ?? []);
    setCertifications(profile.certifications ?? []);
    setProjects(profile.projects ?? []);
  }, [profile]);

  const handleAddSkill = () => {
    const trimmed = newSkillName.trim();
    if (!trimmed) return;
    setSkills((prev) => [
      ...prev,
      {
        name: trimmed,
        category: newSkillCategory,
        level: 'Confirmado pelo candidato',
        evidenceExcerpt: 'Adicionado manualmente pelo candidato',
      },
    ]);
    setNewSkillName('');
  };

  const handleRemoveSkill = (idx: number) => {
    setSkills((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleAddExperience = () => {
    setExperiences((prev) => [
      ...prev,
      {
        company: '',
        role: '',
        startDate: '',
        endDate: '',
        current: false,
        description: '',
        technologies: [],
        evidenceExcerpt: 'Informado pelo candidato',
      },
    ]);
  };

  const handleUpdateExperience = (
    index: number,
    field: keyof ExperienceItemDto,
    value: string | boolean
  ) => {
    setExperiences((prev) =>
      prev.map((exp, i) => (i === index ? { ...exp, [field]: value } : exp))
    );
  };

  const handleRemoveExperience = (index: number) => {
    setExperiences((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAddEducation = () => {
    setEducation((prev) => [
      ...prev,
      {
        institution: '',
        degree: '',
        fieldOfStudy: '',
        startDate: '',
        endDate: '',
        status: 'Concluído',
        evidenceExcerpt: 'Informado pelo candidato',
      },
    ]);
  };

  const handleUpdateEducation = (
    index: number,
    field: keyof EducationItemDto,
    value: string
  ) => {
    setEducation((prev) =>
      prev.map((edu, i) => (i === index ? { ...edu, [field]: value } : edu))
    );
  };

  const handleRemoveEducation = (index: number) => {
    setEducation((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAddLanguage = () => {
    setLanguages((prev) => [
      ...prev,
      {
        language: '',
        proficiency: 'Intermediário',
        evidenceExcerpt: 'Informado pelo candidato',
      },
    ]);
  };

  const handleUpdateLanguage = (
    index: number,
    field: keyof LanguageItemDto,
    value: string
  ) => {
    setLanguages((prev) =>
      prev.map((lang, i) => (i === index ? { ...lang, [field]: value } : lang))
    );
  };

  const handleRemoveLanguage = (index: number) => {
    setLanguages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAddCertification = () => {
    setCertifications((prev) => [
      ...prev,
      {
        name: '',
        issuer: '',
        year: '',
        evidenceExcerpt: 'Informado pelo candidato',
      },
    ]);
  };

  const handleUpdateCertification = (
    index: number,
    field: keyof CertificationItemDto,
    value: string
  ) => {
    setCertifications((prev) =>
      prev.map((cert, i) => (i === index ? { ...cert, [field]: value } : cert))
    );
  };

  const handleRemoveCertification = (index: number) => {
    setCertifications((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAddProject = () => {
    setProjects((prev) => [
      ...prev,
      {
        name: '',
        description: '',
        technologies: [],
        url: '',
        evidenceExcerpt: 'Informado pelo candidato',
      },
    ]);
  };

  const handleUpdateProject = (
    index: number,
    field: keyof ProjectItemDto,
    value: string
  ) => {
    setProjects((prev) =>
      prev.map((proj, i) => (i === index ? { ...proj, [field]: value } : proj))
    );
  };

  const handleRemoveProject = (index: number) => {
    setProjects((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    if (!fullName.trim()) {
      setFeedback({
        type: 'error',
        message: 'O nome completo é obrigatório para salvar o perfil.',
      });
      return;
    }

    const targetRoles = targetRolesInput
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    try {
      await onSaveProfile({
        fullName: fullName.trim(),
        headline: headline.trim(),
        seniorityLevel: seniorityLevel.trim(),
        summary: summary.trim(),
        contactInfo: {
          email: email.trim(),
          phone: phone.trim(),
          location: location.trim(),
          linkedinUrl: linkedinUrl.trim(),
          githubUrl: githubUrl.trim(),
          portfolioUrl: portfolioUrl.trim(),
        },
        experiences,
        education,
        skills,
        languages,
        certifications,
        projects,
        targetRoles,
      });
      setFeedback({
        type: 'success',
        message:
          'Perfil profissional atualizado com sucesso! Avance para a aba "Validação" para confirmar.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        message:
          err instanceof Error
            ? err.message
            : 'Erro ao salvar perfil profissional.',
      });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex flex-col justify-between gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold text-white">
            Perfil Profissional Estruturado (Revisão Completa)
          </h2>
          <p className="mt-1 text-sm text-slate-400">
            Revise, complemente e corrija todas as seções extraídas do seu currículo antes da confirmação explícita.
          </p>
        </div>
        <button
          type="submit"
          data-testid="save-profile-btn"
          disabled={isSaving}
          className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-500 disabled:opacity-50"
        >
          {isSaving ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Save className="h-4 w-4" />
          )}
          {isSaving ? 'Salvando...' : 'Salvar Alterações'}
        </button>
      </div>

      {feedback && (
        <div
          role="alert"
          className={`flex items-center gap-2 rounded-xl border px-4 py-3 text-sm ${
            feedback.type === 'success'
              ? 'border-emerald-800 bg-emerald-950/50 text-emerald-200'
              : 'border-rose-800 bg-rose-950/50 text-rose-200'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
          ) : (
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
          )}
          <span>{feedback.message}</span>
        </div>
      )}

      {/* Dados Principais e Contato */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="space-y-4 rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
            Identificação, Resumo & Senioridade
          </h3>
          <div>
            <label
              htmlFor="profile-fullname"
              className="block text-xs font-medium text-slate-300"
            >
              Nome Completo *
            </label>
            <input
              id="profile-fullname"
              data-testid="profile-fullname-input"
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Ex.: Rafael Almeida"
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label
                htmlFor="profile-headline"
                className="block text-xs font-medium text-slate-300"
              >
                Cargo / Título Profissional
              </label>
              <input
                id="profile-headline"
                type="text"
                value={headline}
                onChange={(e) => setHeadline(e.target.value)}
                placeholder="Ex.: Desenvolvedor Full Stack"
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>

            <div>
              <label
                htmlFor="profile-seniority"
                className="block text-xs font-medium text-slate-300"
              >
                Senioridade
              </label>
              <select
                id="profile-seniority"
                value={seniorityLevel}
                onChange={(e) => setSeniorityLevel(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              >
                <option value="">Selecione...</option>
                <option value="Estágio">Estágio</option>
                <option value="Júnior">Júnior</option>
                <option value="Pleno">Pleno</option>
                <option value="Sênior">Sênior</option>
                <option value="Especialista / Lead">Especialista / Lead</option>
              </select>
            </div>
          </div>

          <div>
            <label
              htmlFor="profile-target-roles"
              className="block text-xs font-medium text-slate-300"
            >
              Áreas / Cargos de Interesse (separados por vírgula)
            </label>
            <input
              id="profile-target-roles"
              type="text"
              value={targetRolesInput}
              onChange={(e) => setTargetRolesInput(e.target.value)}
              placeholder="Desenvolvedor Back-end Júnior, Desenvolvedor Full Stack Júnior"
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label
              htmlFor="profile-summary"
              className="block text-xs font-medium text-slate-300"
            >
              Resumo Profissional
            </label>
            <textarea
              id="profile-summary"
              rows={3}
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder="Resumo objetivo da sua experiência e foco técnico..."
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>
        </div>

        <div className="space-y-4 rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
            Dados de Contato & Links Profissionais
          </h3>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-slate-300">
                E-mail
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                Telefone / WhatsApp
              </label>
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300">
              Localização (Cidade / Estado / Modalidade)
            </label>
            <input
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Ex.: São Paulo, SP (Remoto / Híbrido)"
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-slate-300">
                LinkedIn URL
              </label>
              <input
                type="text"
                value={linkedinUrl}
                onChange={(e) => setLinkedinUrl(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300">
                GitHub URL
              </label>
              <input
                type="text"
                value={githubUrl}
                onChange={(e) => setGithubUrl(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300">
              Portfólio / Site Pessoal URL
            </label>
            <input
              type="text"
              value={portfolioUrl}
              onChange={(e) => setPortfolioUrl(e.target.value)}
              placeholder="https://meuportfolio.dev"
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>
        </div>
      </div>

      {/* Competências Técnicas e Comportamentais */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
          Competências Técnicas e Comportamentais ({skills.length})
        </h3>
        <div className="mt-3 flex flex-wrap gap-2">
          {skills.map((skill, idx) => (
            <span
              key={`${skill.name}-${idx}`}
              className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs font-medium text-slate-200"
            >
              <span>{skill.name}</span>
              <span className="text-[10px] text-slate-400">
                ({skill.category})
              </span>
              <button
                type="button"
                onClick={() => handleRemoveSkill(idx)}
                className="text-slate-400 hover:text-rose-400"
              >
                <Trash2 className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <input
            type="text"
            value={newSkillName}
            onChange={(e) => setNewSkillName(e.target.value)}
            placeholder="Adicionar competência (ex.: Docker, AWS, Comunicação)..."
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs text-white focus:border-indigo-500 focus:outline-none"
          />
          <select
            value={newSkillCategory}
            onChange={(e) => setNewSkillCategory(e.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs text-white"
          >
            <option value="LANGUAGE_TECH">Linguagem</option>
            <option value="FRAMEWORK">Framework</option>
            <option value="TOOL">Ferramenta / Cloud</option>
            <option value="HARD_SKILL">Competência Técnica</option>
            <option value="SOFT_SKILL">Competência Comportamental</option>
          </select>
          <button
            type="button"
            onClick={handleAddSkill}
            className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700"
          >
            <Plus className="h-3.5 w-3.5" />
            Adicionar
          </button>
        </div>
      </div>

      {/* Experiências Profissionais */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
            Experiências Profissionais ({experiences.length})
          </h3>
          <button
            type="button"
            onClick={handleAddExperience}
            className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700"
          >
            <Plus className="h-3.5 w-3.5" />
            Adicionar Experiência
          </button>
        </div>

        <div className="mt-4 space-y-4">
          {experiences.map((exp, idx) => (
            <div
              key={idx}
              className="rounded-xl border border-slate-800 bg-slate-950/60 p-4"
            >
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-4">
                <input
                  type="text"
                  value={exp.role}
                  onChange={(e) =>
                    handleUpdateExperience(idx, 'role', e.target.value)
                  }
                  placeholder="Cargo"
                  className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
                />
                <input
                  type="text"
                  value={exp.company}
                  onChange={(e) =>
                    handleUpdateExperience(idx, 'company', e.target.value)
                  }
                  placeholder="Empresa"
                  className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
                />
                <input
                  type="text"
                  value={exp.startDate}
                  onChange={(e) =>
                    handleUpdateExperience(idx, 'startDate', e.target.value)
                  }
                  placeholder="Início (ex.: 2023)"
                  className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
                />
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={exp.endDate}
                    onChange={(e) =>
                      handleUpdateExperience(idx, 'endDate', e.target.value)
                    }
                    placeholder="Fim / Atual"
                    className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
                  />
                  <button
                    type="button"
                    onClick={() => handleRemoveExperience(idx)}
                    className="text-slate-400 hover:text-rose-400"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
              <textarea
                rows={2}
                value={exp.description}
                onChange={(e) =>
                  handleUpdateExperience(idx, 'description', e.target.value)
                }
                placeholder="Principais atividades e resultados alcançados..."
                className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
              />
            </div>
          ))}
        </div>
      </div>

      {/* Formação Acadêmica */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold uppercase tracking-wider text-indigo-400">
            Formação Acadêmica ({education.length})
          </h3>
          <button
            type="button"
            onClick={handleAddEducation}
            className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium text-white hover:bg-slate-700"
          >
            <Plus className="h-3.5 w-3.5" />
            Adicionar Formação
          </button>
        </div>

        <div className="mt-4 space-y-3">
          {education.map((edu, idx) => (
            <div
              key={idx}
              className="grid grid-cols-1 gap-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4 sm:grid-cols-3"
            >
              <input
                type="text"
                value={edu.degree}
                onChange={(e) =>
                  handleUpdateEducation(idx, 'degree', e.target.value)
                }
                placeholder="Curso / Grau (ex.: Bacharelado em Ciência da Computação)"
                className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
              />
              <input
                type="text"
                value={edu.institution}
                onChange={(e) =>
                  handleUpdateEducation(idx, 'institution', e.target.value)
                }
                placeholder="Instituição"
                className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
              />
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={edu.status}
                  onChange={(e) =>
                    handleUpdateEducation(idx, 'status', e.target.value)
                  }
                  placeholder="Status / Ano"
                  className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs text-white"
                />
                <button
                  type="button"
                  onClick={() => handleRemoveEducation(idx)}
                  className="text-slate-400 hover:text-rose-400"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Idiomas, Certificações e Projetos (Seção 5 — Tela 3 Completa) */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Idiomas */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-indigo-400">
              Idiomas ({languages.length})
            </h3>
            <button
              type="button"
              onClick={handleAddLanguage}
              className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-700"
            >
              <Plus className="h-3 w-3" />
              Idioma
            </button>
          </div>
          <div className="mt-3 space-y-2.5">
            {languages.map((lang, idx) => (
              <div
                key={idx}
                className="flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-950/60 p-2.5"
              >
                <input
                  type="text"
                  value={lang.language}
                  onChange={(e) =>
                    handleUpdateLanguage(idx, 'language', e.target.value)
                  }
                  placeholder="Ex.: Inglês"
                  className="w-1/2 rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                />
                <input
                  type="text"
                  value={lang.proficiency}
                  onChange={(e) =>
                    handleUpdateLanguage(idx, 'proficiency', e.target.value)
                  }
                  placeholder="Nível"
                  className="w-1/2 rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                />
                <button
                  type="button"
                  onClick={() => handleRemoveLanguage(idx)}
                  className="text-slate-400 hover:text-rose-400"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Certificações e Cursos */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-indigo-400">
              Certificações & Cursos ({certifications.length})
            </h3>
            <button
              type="button"
              onClick={handleAddCertification}
              className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-700"
            >
              <Plus className="h-3 w-3" />
              Curso
            </button>
          </div>
          <div className="mt-3 space-y-2.5">
            {certifications.map((cert, idx) => (
              <div
                key={idx}
                className="space-y-1.5 rounded-xl border border-slate-800 bg-slate-950/60 p-2.5"
              >
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={cert.name}
                    onChange={(e) =>
                      handleUpdateCertification(idx, 'name', e.target.value)
                    }
                    placeholder="Nome da certificação / curso"
                    className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                  />
                  <button
                    type="button"
                    onClick={() => handleRemoveCertification(idx)}
                    className="text-slate-400 hover:text-rose-400"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
                <input
                  type="text"
                  value={cert.issuer}
                  onChange={(e) =>
                    handleUpdateCertification(idx, 'issuer', e.target.value)
                  }
                  placeholder="Instituição emissora"
                  className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                />
              </div>
            ))}
          </div>
        </div>

        {/* Projetos */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-indigo-400">
              Projetos ({projects.length})
            </h3>
            <button
              type="button"
              onClick={handleAddProject}
              className="inline-flex items-center gap-1 rounded-lg bg-slate-800 px-2.5 py-1 text-xs font-medium text-white hover:bg-slate-700"
            >
              <Plus className="h-3 w-3" />
              Projeto
            </button>
          </div>
          <div className="mt-3 space-y-2.5">
            {projects.map((proj, idx) => (
              <div
                key={idx}
                className="space-y-1.5 rounded-xl border border-slate-800 bg-slate-950/60 p-2.5"
              >
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={proj.name}
                    onChange={(e) =>
                      handleUpdateProject(idx, 'name', e.target.value)
                    }
                    placeholder="Nome do projeto"
                    className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                  />
                  <button
                    type="button"
                    onClick={() => handleRemoveProject(idx)}
                    className="text-slate-400 hover:text-rose-400"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
                <input
                  type="text"
                  value={proj.description}
                  onChange={(e) =>
                    handleUpdateProject(idx, 'description', e.target.value)
                  }
                  placeholder="Descrição / tecnologias"
                  className="w-full rounded-lg border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs text-white"
                />
              </div>
            ))}
          </div>
        </div>
      </div>
    </form>
  );
};
