-- V5__security_and_discovery_diagnostics.sql
-- Autenticação real por usuário (BCrypt + JWT assinado) e telemetria reproduzível de sincronização/descoberta

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

ALTER TABLE search_configs
    ADD COLUMN IF NOT EXISTS last_execution_diagnostics JSONB NOT NULL DEFAULT '[]'::jsonb;
