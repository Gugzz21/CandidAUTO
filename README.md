# CandidAUTO — Plataforma Inteligente de Busca e Candidatura a Vagas com IA

Aplicação web completa desenvolvida sob a arquitetura **Personal First, SaaS Ready** que atua como um **agente inteligente de recrutamento e candidaturas**: analisa currículos (PDF e DOCX) com rastreamento de evidências sem alucinação, descobre oportunidades reais automaticamente em múltiplas plataformas públicas, calcula compatibilidade de `0 a 100%`, prepara o preenchimento inteligente de formulários com Cofre de Respostas Confirmadas e exige aprovação explícita do candidato antes de qualquer envio.

---

## Stack Tecnológica

| Camada | Tecnologia |
| :--- | :--- |
| **Backend** | Java 21 + Spring Boot 3.5 + Spring Security (BCrypt + JWT HMAC-SHA256) |
| **Frontend** | React 19 + TypeScript + Vite 7 |
| **Banco de Dados** | PostgreSQL 16 + Spring Data JPA / Hibernate |
| **Migrações** | Flyway (`V1` a `V5`) |
| **Extração de Currículo** | Apache PDFBox 3 (PDF) + Apache POI 5 (DOCX) |
| **Estilização** | Tailwind CSS 4 + Lucide Icons |
| **Testes & CI/CD** | JUnit 5 + MockMvc + PostgreSQL 16 (Backend) / Vitest + Testing Library (Frontend) / GitHub Actions CI |

---

## Funcionalidades Implementadas (Etapas 1 a 6 + Segurança & Observabilidade)

1. **Etapa 1 — Currículo e Perfil Estruturado com Evidências:** Upload de PDF/DOCX, extração local, estruturação por IA (compatível com NVIDIA NIM / OpenAI / Ollama + fallback determinístico offline) sem inventar qualificações, editor manual completo e confirmação explícita do perfil.
2. **Etapa 2 — Job Discovery Engine, Conectores Oficiais e Telemetria Reproduzível:**
   - **Descoberta ativa sem depender de fontes manuais (`JobDiscoveryEngine`):** Pesquisa automaticamente oportunidades pelos cargos, modalidades e senioridades configurados em **Gupy Portal Público**, **Remotive Public API**, **Arbeitnow Public API** e **Catálogo Curado Greenhouse & Lever**, filtrando vagas fora de escopo (`JobRelevanceFilter`).
   - **Conectores dedicados por empresa:** **Greenhouse**, **Lever**, **Gupy** e **Páginas de Carreiras (Schema.org `JobPosting` JSON-LD / `__NEXT_DATA__`)**, com proteção contra expiração prematura em falhas de extração (`SyncOutcomeStatus`).
   - **Importação assistida respeitando termos de uso:** Fluxo dedicado para **LinkedIn, Indeed, Glassdoor, Programathor, Revelo e InfoJobs**, com deduplicação SHA-256 (`fingerprint_hash`).
   - **Diagnóstico reproduzível por conector (`GET /api/v1/jobs/diagnostics`):** Registra URL consultada, código HTTP, status da extração (`SUCCESS_WITH_RESULTS`, `SUCCESS_EMPTY_CONFIRMED`, `EXTRACTION_INCONCLUSIVE`, `PARSE_ERROR`, `HTTP_ERROR`, `NETWORK_ERROR`), total extraído pelo parser, total descartado com motivos exatos e total inserido/atualizado.
3. **Etapa 3 — Motor de Compatibilidade (0 a 100%) e Painel de Oportunidades:** Avaliação multicritério explicável (pontos positivos, pontos de atenção, requisitos atendidos e não atendidos) sem eliminar vagas por requisitos desejáveis.
4. **Etapa 4 — Preenchimento Inteligente e Cofre do Candidato (`candidate_qa_vault`):** Autopreenchimento de formulários exclusivamente com dados confirmados e detecção automática de perguntas adicionais ausentes (`missingQuestions`).
5. **Etapa 5 — Tela de Revisão Final e Aprovação Explícita:** Botões **CANDIDATAR-SE** (exige confirmação humana explícita, gera recibo auditável e registra data/hora) e **CANCELAR** (interrompe sem enviar dados).
6. **Etapa 6 — Acompanhamento (11 Status), Agendador `@Scheduled` e Indicadores:** Agendador periódico real em segundo plano (`JobMonitoringScheduler`) respeitando `intervalMinutes` por usuário, gestão pelos 11 status oficiais e 6 indicadores no Dashboard Principal.
7. **Segurança e Isolamento Multi-Usuário:** Autenticação stateless real (`/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/local-session`, `/api/v1/auth/me`) com senhas BCrypt, tokens JWT assinados via HMAC-SHA256 e bloqueio estrito (`403 Forbidden`) contra falsificação de cabeçalho `X-User-Id`.

---

## Configuração de Ambiente (`.env`)

1. Copie `.env.example` para `.env` na raiz do repositório (o `.env` é ignorado pelo Git):
   ```bash
   cp .env.example .env
   ```
2. Defina suas credenciais locais no `.env`:
   - `DB_PASSWORD`: senha do banco PostgreSQL local.
   - `JWT_SECRET`: chave secreta HMAC-SHA256 para assinatura de sessões JWT.
   - `CandidAUTO_API_KEY`: chave de API para o provedor LLM compatível com OpenAI/NVIDIA NIM (opcional; sem chave, o analisador determinístico local permanece operacional).

---

## Como Executar Localmente

### 1. Banco de Dados PostgreSQL 16
```bash
./infra/start-db.sh
```
*(Ou via Docker Compose definindo `DB_PASSWORD` no `.env`: `docker compose up -d`).*

### 2. Backend (Spring Boot em `http://localhost:8080`)
```bash
cd backend
mvn spring-boot:run
```

### 3. Frontend (React + Vite em `http://localhost:5173`)
```bash
cd frontend
npm run dev
```

---

## Executando os Testes Automatizados

- **Backend (19 testes JUnit 5 + Integração PostgreSQL/Flyway + Segurança JWT + Job Discovery Engine):**
  ```bash
  mvn -f backend/pom.xml test
  ```
- **Frontend (9 testes Vitest + Testing Library + Build TypeScript):**
  ```bash
  npm --prefix frontend run test
  npm --prefix frontend run build
  ```

---

## Documentação Adicional

- [Arquitetura do Sistema](docs/architecture.md)
- [Modelagem do Banco de Dados](docs/database.md)
- [Contratos da API REST](docs/api.md)
