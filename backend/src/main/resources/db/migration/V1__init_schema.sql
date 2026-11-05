-- V1__init_schema.sql
-- Esquema inicial da Etapa 1: Usuários, Currículos, Perfis Profissionais e Evidências

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    file_type VARCHAR(30) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    extracted_text TEXT,
    status VARCHAR(40) NOT NULL,
    error_message TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_uploaded_at ON resumes(uploaded_at DESC);

CREATE TABLE candidate_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id UUID REFERENCES resumes(id) ON DELETE SET NULL,
    full_name VARCHAR(200),
    headline VARCHAR(255),
    seniority_level VARCHAR(60),
    summary TEXT,
    contact_info JSONB NOT NULL DEFAULT '{}'::jsonb,
    experiences JSONB NOT NULL DEFAULT '[]'::jsonb,
    education JSONB NOT NULL DEFAULT '[]'::jsonb,
    skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    languages JSONB NOT NULL DEFAULT '[]'::jsonb,
    certifications JSONB NOT NULL DEFAULT '[]'::jsonb,
    projects JSONB NOT NULL DEFAULT '[]'::jsonb,
    target_roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_candidate_profiles_user_id ON candidate_profiles(user_id);

CREATE TABLE profile_evidences (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    field_path VARCHAR(160) NOT NULL,
    field_label VARCHAR(200) NOT NULL,
    extracted_value TEXT,
    evidence_text TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_evidences_profile_id ON profile_evidences(profile_id);

-- Usuário padrão para modo Personal First (mantendo isolamento por user_id para SaaS Ready)
INSERT INTO users (id, name, email, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Candidato Principal (Personal First)',
    'candidato@jobassistant.local',
    NOW()
) ON CONFLICT (id) DO NOTHING;
