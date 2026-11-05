-- V3__job_matching_analysis.sql
-- Etapa 3: Análise Inteligente de Compatibilidade (0 a 100%) e Explicações Detalhadas

CREATE TABLE job_match_analyses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES candidate_profiles(id) ON DELETE SET NULL,
    compatibility_score INT NOT NULL CHECK (compatibility_score >= 0 AND compatibility_score <= 100),
    score_breakdown JSONB NOT NULL DEFAULT '{}'::jsonb,
    positive_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    attention_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    matched_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    ai_summary TEXT NOT NULL,
    analyzed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_job_match_user_job UNIQUE (user_id, job_id)
);

CREATE INDEX idx_job_match_user_score ON job_match_analyses(user_id, compatibility_score DESC);
CREATE INDEX idx_job_match_job_id ON job_match_analyses(job_id);
