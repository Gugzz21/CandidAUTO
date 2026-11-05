-- V2__jobs_and_sources.sql
-- Etapa 2: Fontes de Oportunidade, Descoberta de Vagas, Normalização e Deduplicação

CREATE TABLE job_sources (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    board_identifier VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at TIMESTAMPTZ,
    last_sync_status VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_sources_user_id ON job_sources(user_id);

CREATE TABLE job_postings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_id UUID REFERENCES job_sources(id) ON DELETE SET NULL,
    fingerprint_hash VARCHAR(128) NOT NULL,
    external_id VARCHAR(255),
    source_platform VARCHAR(40) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description_raw TEXT NOT NULL,
    mandatory_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    desirable_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    technologies JSONB NOT NULL DEFAULT '[]'::jsonb,
    seniority VARCHAR(60),
    work_modality VARCHAR(40) NOT NULL DEFAULT 'NAO_INFORMADO',
    location VARCHAR(255),
    salary_range VARCHAR(160),
    benefits JSONB NOT NULL DEFAULT '[]'::jsonb,
    application_questions JSONB NOT NULL DEFAULT '[]'::jsonb,
    original_url VARCHAR(1000) NOT NULL,
    published_at TIMESTAMPTZ,
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_job_postings_user_fingerprint UNIQUE (user_id, fingerprint_hash)
);

CREATE INDEX idx_job_postings_user_id ON job_postings(user_id);
CREATE INDEX idx_job_postings_platform ON job_postings(source_platform);
CREATE INDEX idx_job_postings_active ON job_postings(active);
