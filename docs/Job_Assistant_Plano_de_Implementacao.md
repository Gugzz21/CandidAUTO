# Job Assistant — Plano de Implementação (Etapa 1 Concluída)

> Documento técnico de referência para implementar a primeira versão funcional do Job Assistant.
> **Status atual:** Etapa 1 (E1 a E5) implementada e comprovada por execução, banco PostgreSQL + Flyway e testes automatizados.
> **Estratégia:** Personal First, SaaS Ready.

## Critérios de conclusão da E1 — Ambiente
- [x] Repositório inicializado (`git init`).
- [x] Frontend executando (`React 19 + TypeScript + Vite 7 + Tailwind CSS 4`).
- [x] Backend executando (`Java 21 + Spring Boot 3.5.10`).
- [x] PostgreSQL acessível pela aplicação (`PostgreSQL 16` + `Flyway`).
- [x] Configurações sensíveis fora do controle de versão (`.gitignore` + `.env.example`).
- [x] Instruções de execução documentadas (`README.md`, `docs/architecture.md`, `docs/database.md`, `docs/api.md`).

## Critérios gerais de aceitação da Etapa 1
- [x] Frontend e backend executam localmente.
- [x] Banco persiste os dados (`users`, `resumes`, `candidate_profiles`, `profile_evidences`).
- [x] Migrações Flyway executam corretamente (`V1__init_schema.sql`).
- [x] Upload de PDF e DOCX funciona com validação de extensão, MIME type, magic bytes e tamanho.
- [x] Texto dos arquivos é extraído corretamente nos formatos suportados (`Apache PDFBox 3.0.4` e `Apache POI 5.4.0`).
- [x] Resposta da IA é estruturada e validada atrás da interface `LlmClient`.
- [x] Campos do perfil possuem evidências rastreáveis (`verified = true`) ou indicação de ausência de evidência (`needsReview = true`).
- [x] Usuário consegue revisar e editar o perfil (`PUT /api/v1/profile`).
- [x] Usuário consegue confirmar explicitamente o perfil (`POST /api/v1/profile/confirm`).
- [x] Testes automatizados relevantes passam (`10/10` testes JUnit 5 no backend + `5/5` testes Vitest no frontend).
- [x] README explica instalação, configuração e execução.

## Registro de progresso (Seção 12)

| Entrega | Status | Evidência / observação |
| :--- | :--- | :--- |
| **E1 — Ambiente** | **Concluído** | Repositório Git inicializado, `compose.yaml`, `.env.example`, `.gitignore`, Backend Spring Boot 3.5.10 e Frontend Vite 7.3.2 compilando e iniciando localmente. |
| **E2 — Persistência** | **Concluído** | Migração Flyway `V1__init_schema.sql` aplicada no PostgreSQL 16; entidades JPA (`UserEntity`, `ResumeEntity`, `CandidateProfileEntity`, `ProfileEvidenceEntity`) e isolamento estrito por `user_id` testados em `JobAssistantIntegrationTest`. |
| **E3 — Currículo** | **Concluído** | `ResumeStorageService` (armazenamento privado com `storage_key` UUID e verificação de assinatura binária) e `ResumeTextExtractor` (`Apache PDFBox` + `Apache POI`), validados em `ResumeStorageServiceTest` e `ResumeTextExtractorTest`. |
| **E4 — IA** | **Concluído** | Interface `LlmClient`, `OpenAiCompatibleLlmClient`, `DeterministicResumeAnalyzer` e `AiProfileAnalysisService` com verificação literal de evidência contra o texto do currículo (`AiProfileAnalysisServiceTest`). |
| **E5 — Interface** | **Concluído** | Interface web em tema escuro SaaS com as 5 abas (`Dashboard`, `Currículo`, `Análise IA`, `Perfil`, `Validação`), testada com `Vitest` (`src/app/App.test.tsx` — 5/5 testes passando) e build de produção (`vite build`). |
