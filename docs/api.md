# Contratos da API REST (v1) — Etapas 1 a 6


Todos os endpoints utilizam DTOs dedicados e isolam os dados pelo escopo de `user_id` (suportando cabeçalho opcional `X-User-Id` para testes multiusuário).

## Etapa 1 — Currículos e Perfil Profissional

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/resumes` | Faz upload de currículo (`multipart/form-data`, campo `file` em PDF ou DOCX), valida assinatura binária, armazena com `storage_key` interno e extrai o texto. |
| `GET` | `/api/v1/resumes` | Lista os currículos enviados pelo usuário atual em ordem decrescente de envio. |
| `GET` | `/api/v1/resumes/{id}` | Consulta detalhes e estado de processamento de um currículo específico. |
| `POST` | `/api/v1/resumes/{id}/analyze` | Solicita análise por IA do texto extraído, estruturando o perfil e gerando evidências verificadas (`REVIEW_REQUIRED`). |
| `GET` | `/api/v1/profile` | Consulta o perfil profissional estruturado e a lista de evidências (`evidences`). |
| `PUT` | `/api/v1/profile` | Atualiza manualmente o perfil profissional estruturado (`UpdateProfileRequestDto`). |
| `POST` | `/api/v1/profile/confirm` | Confirma explicitamente os dados revisados pelo usuário (`confirmed = true`), atualizando também o status do currículo vinculado para `CONFIRMED`. |

## Etapa 2 — Fontes de Emprego, Conectores e Descoberta de Vagas

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/v1/job-sources` | Lista as fontes de vagas cadastradas pelo usuário (`GREENHOUSE`, `LEVER`, `GUPY`, `CAREER_PAGE`). |
| `POST` | `/api/v1/job-sources` | Cadastra uma nova fonte de vagas (`CreateJobSourceRequestDto`). |
| `DELETE` | `/api/v1/job-sources/{id}` | Remove uma fonte de vagas do usuário. |
| `POST` | `/api/v1/job-sources/{id}/sync` | Sincroniza vagas reais da fonte via conector (`GreenhouseJobConnector`, `LeverJobConnector`, `CareerPageJsonLdConnector`), normalizando, deduplicando por `fingerprint_hash` e marcando vagas removidas como expiradas (`active = false`). |
| `POST` | `/api/v1/jobs/import` | Importa e normaliza uma oportunidade assistida por link e descrição (`LINKEDIN`, `INDEED`, `GUPY`, `GLASSDOOR`, `PROGRAMATHOR`, `REVELO`, `INFOJOBS`), extraindo requisitos obrigatórios, desejáveis, tecnologias, modalidade e faixa salarial. |
| `GET` | `/api/v1/jobs` | Lista as vagas normalizadas do usuário com suporte a filtros (`platform`, `modality`, `activeOnly`, `search`). |
| `GET` | `/api/v1/jobs/{id}` | Consulta os detalhes completos de uma oportunidade. |
| `PATCH` | `/api/v1/jobs/{id}/availability` | Marca uma oportunidade como ativa ou expirada/indisponível (`{ "active": false }`). |

## Etapa 3 — Motor de Compatibilidade (0 a 100%) e Painel de Oportunidades

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/jobs/{id}/match` | Analisa a compatibilidade (0 a 100%) de uma vaga específica contra o perfil profissional, detalhando pontos positivos, pontos de atenção, requisitos atendidos e não atendidos. |
| `POST` | `/api/v1/jobs/match-all` | Executa a análise de compatibilidade em lote para todas as vagas ativas do usuário e ordena por maior pontuação. |
| `GET` | `/api/v1/opportunities` | Retorna os cards de oportunidades ranqueados por compatibilidade, com suporte aos 9 filtros (`minScore`, `area`, `seniority`, `company`, `salary`, `modality`, `location`, `platform`, `status`). |

## Etapas 4, 5 e 6 — Preenchimento Inteligente, Revisão Obrigatória, Acompanhamento e Automação

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/applications/prepare/{jobId}` | Prepara a candidatura inteligente preenchendo automaticamente os campos compatíveis com dados verdadeiros do perfil/cofre e detectando perguntas adicionais ausentes (`missingQuestions`). |
| `PUT` | `/api/v1/applications/{id}/answers` | Salva as respostas verdadeiras fornecidas pelo candidato para as perguntas ausentes e atualiza opcionalmente o Cofre de Respostas Confirmadas (`candidate_qa_vault`). |
| `POST` | `/api/v1/applications/{id}/submit` | **BOTÃO: CANDIDATAR-SE** — Exige `explicitApprovalConfirmed = true`, valida que não há perguntas obrigatórias pendentes, registra `submittedAt`, comprovante e atualiza para `CANDIDATURA_ENVIADA`. |
| `POST` | `/api/v1/applications/{id}/cancel` | **BOTÃO: CANCELAR** — Interrompe o processo sem enviar dados à empresa, mantendo como rascunho (`AGUARDANDO_REVISAO`) ou marcando como `CANCELADA`. |
| `PATCH` | `/api/v1/applications/{id}/status` | Atualiza o status da candidatura pelos 11 estados do ciclo de vida (`ENCONTRADA`, `EM_ANALISE_IA`, `AGUARDANDO_REVISAO`, `PRONTA_PARA_CANDIDATURA`, `CANDIDATURA_ENVIADA`, `EM_ANDAMENTO`, `ENTREVISTA_AGENDADA`, `APROVADO`, `REPROVADO`, `CANCELADA`, `VAGA_ENCERRADA`). |
| `GET` | `/api/v1/applications` | Lista o histórico de candidaturas do usuário com filtro opcional por `status`. |
| `GET` | `/api/v1/qa-vault` | Lista as respostas confirmadas no Cofre do Candidato. |
| `GET` | `/api/v1/search-config` | Retorna a configuração de cargos procurados e monitoramento automático. |
| `PUT` | `/api/v1/search-config` | Atualiza os cargos procurados, modalidades, score mínimo e intervalo de monitoramento. |
| `POST` | `/api/v1/search-config/run-now` | Executa imediatamente um ciclo de busca e análise de compatibilidade sem repetir vagas já analisadas ou candidatadas. |
| `GET` | `/api/v1/dashboard/metrics` | Retorna os 6 indicadores (KPIs) do Dashboard Principal (`totalJobsFound`, `jobsAnalyzedByAi`, `highCompatibilityJobs`, `applicationsSubmitted`, `interviewsScheduled`, `conversionRatePercent`). |
