# Modelagem do Banco de Dados (PostgreSQL 16 + Flyway)

O esquema relacional é gerenciado pelas migrações Flyway `V1` a `V5` em `backend/src/main/resources/db/migration/`.

## Tabelas Principais

1. **`users`** (`V1__init_schema.sql`, expandida em `V5__security_and_discovery_diagnostics.sql`)
   - `id` (`UUID` PK), `name`, `email` (`UNIQUE`, indexado case-insensitive em `LOWER(email)`), `password_hash` (`VARCHAR(255)` BCrypt), `created_at`

2. **`resumes`** (`V1__init_schema.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `filename`, `storage_key` (`UNIQUE`), `file_type` (`PDF` / `DOCX`), `file_size_bytes`, `extracted_text`, `status`, `error_message`, `uploaded_at`

3. **`candidate_profiles`** (`V1__init_schema.sql`)
   - `id` (`UUID` PK), `user_id` (`UNIQUE FK -> users.id`), `resume_id`, `full_name`, `headline`, `seniority_level`, `summary`
   - Colunas `JSONB`: `contact_info`, `experiences`, `education`, `skills`, `languages`, `certifications`, `projects`, `target_roles`
   - `confirmed` (`BOOLEAN`), `confirmed_at`, `updated_at`

4. **`profile_evidences`** (`V1__init_schema.sql`)
   - `id` (`UUID` PK), `profile_id` (`FK -> candidate_profiles.id`), `field_path`, `field_label`, `extracted_value`, `evidence_text`, `verified`, `needs_review`, `created_at`

5. **`job_sources`** (`V2__jobs_and_sources.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `name`, `platform`, `board_identifier`, `enabled`, `last_synced_at`, `last_sync_status`, `created_at`

6. **`job_postings`** (`V2__jobs_and_sources.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `source_id` (`FK -> job_sources.id`), `external_id`, `source_platform`, `company_name`, `title`, `description_raw`
   - Colunas `JSONB`: `mandatory_requirements`, `desirable_requirements`, `technologies`, `benefits`, `application_questions`
   - `seniority`, `work_modality`, `location`, `salary_range`, `original_url`, `fingerprint_hash` (`UNIQUE (user_id, fingerprint_hash)`), `published_at`, `discovered_at`, `active`

7. **`job_match_analyses`** (`V3__job_matching_analysis.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `job_id` (`FK -> job_postings.id`), `profile_id` (`FK -> candidate_profiles.id`), `compatibility_score` (`0..100`)
   - Colunas `JSONB`: `score_breakdown`, `positive_points`, `attention_points`, `matched_requirements`, `missing_requirements`
   - `ai_summary`, `analyzed_at`, constraint `UNIQUE (user_id, job_id)`

8. **`candidate_qa_vault`** (`V4__applications_and_automation.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `question_key`, `question_label`, `confirmed_answer`, `updated_at`, constraint `UNIQUE (user_id, question_key)`

9. **`applications`** (`V4__applications_and_automation.sql`)
   - `id` (`UUID` PK), `user_id` (`FK -> users.id`), `job_id` (`FK -> job_postings.id`), `status` (11 estados oficiais), `submission_mode` (`INTEGRACAO_AUTORIZADA` / `FLUXO_ASSISTIDO`)
   - Colunas `JSONB`: `prepared_fields`, `missing_questions`, `audit_log`
   - `user_approved_explicitly` (`BOOLEAN`), `submitted_at`, `submission_receipt`, `notes`, `created_at`, `updated_at`, constraint `UNIQUE (user_id, job_id)`

10. **`search_configs`** (`V4__applications_and_automation.sql`, expandida em `V5__security_and_discovery_diagnostics.sql`)
    - `id` (`UUID` PK), `user_id` (`UNIQUE FK -> users.id`), `desired_roles` (`JSONB`), `desired_modalities` (`JSONB`), `desired_seniorities` (`JSONB`), `min_compatibility_score`, `auto_monitoring_enabled`, `interval_minutes`, `last_run_at`, `last_run_summary`, `last_execution_diagnostics` (`JSONB`), `updated_at`
