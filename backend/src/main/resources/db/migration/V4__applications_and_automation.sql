-- V4__applications_and_automation.sql
-- Etapas 4, 5 e 6: Preenchimento Inteligente, Cofre de Respostas Confirmadas, Aprovação Explícita, 11 Status e Monitoramento Automático

CREATE TABLE candidate_qa_vault (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_key VARCHAR(200) NOT NULL,
    question_label VARCHAR(300) NOT NULL,
    confirmed_answer TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_qa_vault_user_key UNIQUE (user_id, question_key)
);

CREATE INDEX idx_qa_vault_user_id ON candidate_qa_vault(user_id);

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    submission_mode VARCHAR(40) NOT NULL DEFAULT 'FLUXO_ASSISTIDO',
    prepared_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_questions JSONB NOT NULL DEFAULT '[]'::jsonb,
    user_approved_explicitly BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMPTZ,
    submission_receipt TEXT,
    notes TEXT,
    audit_log JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_applications_user_job UNIQUE (user_id, job_id)
);

CREATE INDEX idx_applications_user_status ON applications(user_id, status);
CREATE INDEX idx_applications_job_id ON applications(job_id);

CREATE TABLE search_configs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    desired_roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    desired_modalities JSONB NOT NULL DEFAULT '[]'::jsonb,
    desired_seniorities JSONB NOT NULL DEFAULT '[]'::jsonb,
    min_compatibility_score INT NOT NULL DEFAULT 70,
    auto_monitoring_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    interval_minutes INT NOT NULL DEFAULT 60,
    last_run_at TIMESTAMPTZ,
    last_run_summary TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
