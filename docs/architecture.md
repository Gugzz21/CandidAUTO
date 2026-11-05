# Arquitetura do Sistema — CandidAUTO (Etapas 1 a 6)

## Monólito Modular (Personal First, SaaS Ready)

O **CandidAUTO** utiliza uma arquitetura de **Monólito Modular** em Java 21 + Spring Boot 3.5 no backend e React 19 + TypeScript + Vite 7 no frontend:

- **`profile` (`com.jobassistant.profile`):** Gerenciamento do perfil profissional estruturado, evidências rastreáveis, edição manual e confirmação explícita do candidato.
- **`resume` (`com.jobassistant.resume`):** Upload seguro, validação binária (magic bytes), armazenamento privado fora do controle de versão (`storage_key` interno UUID), extração de texto com Apache PDFBox (PDF) e Apache POI (DOCX), e máquina de estados do processamento.
- **`ai` (`com.jobassistant.ai`):** Interface desacoplada `LlmClient`, construção de prompts com delimitação de dados não confiáveis (`<untrusted_resume_text>`), cliente HTTP compatível com OpenAI/NVIDIA NIM + fallback local determinístico (`DeterministicResumeAnalyzer`) e verificador de evidências contra o texto original do currículo.
- **`jobs` (`com.jobassistant.jobs`):**
  - **Descoberta ativa (`discovery`):** `JobDiscoveryEngine`, `JobRelevanceFilter` e provedores públicos (`GupyPublicSearchProvider`, `RemotivePublicSearchProvider`, `ArbeitnowPublicSearchProvider`, `CuratedBoardsDiscoveryProvider`) orientados por cargos, senioridades, modalidades e skills do usuário.
  - **Conectores por fonte (`connector`):** `GreenhouseJobConnector`, `LeverJobConnector`, `GupyJobConnector` e `CareerPageJsonLdConnector` com classificação estrita de resultado (`SyncOutcomeStatus`), proteção contra expiração prematura e telemetria reproduzível (`ConnectorDiagnosticDto`).
  - **Importação assistida e deduplicação:** Fluxo seguro para LinkedIn/Indeed/Glassdoor e deduplicação SHA-256 (`fingerprint_hash`).
- **`matching` (`com.jobassistant.matching`):** Motor de compatibilidade de `0 a 100%` (`JobMatchingEngineService`) com explicação detalhada (pontos positivos, pontos de atenção, requisitos atendidos/não atendidos) e painel filtrável (`OpportunitiesController`).
- **`applications` (`com.jobassistant.applications`):** Preenchimento inteligente (`SmartApplicationService`), Cofre de Respostas Confirmadas (`candidate_qa_vault`), detecção de perguntas ausentes, revisão obrigatória (`CANDIDATAR-SE` / `CANCELAR`) e acompanhamento pelos 11 status.
- **`automation` (`com.jobassistant.automation`):** Agendador periódico real em segundo plano (`SchedulingConfig` + `JobMonitoringScheduler` com `@Scheduled`), respeitando `intervalMinutes` por usuário sem depender de requisições HTTP, além do cálculo dos 6 indicadores do Dashboard Principal (`AutomatedJobMonitoringService`).
- **`security` (`com.jobassistant.security`):** Autenticação stateless real (`AuthController`, `AuthTokenService` HMAC-SHA256, `JwtAuthenticationFilter` e `BCryptPasswordEncoder`), bloqueando falsificação de cabeçalho `X-User-Id` (`403 Forbidden`) e isolando dados por `user_id` via `CurrentUserProvider`.
- **`shared` (`com.jobassistant.shared`):** Tratamento global e padronizado de exceções (`GlobalExceptionHandler`).
