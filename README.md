# Job Assistant — Plataforma Inteligente de Busca e Candidatura a Vagas com IA

Aplicação web desenvolvida sob a estratégia **Personal First, SaaS Ready** para organização do perfil profissional, extração e estruturação de currículos (PDF e DOCX) com rastreamento de evidências por IA e revisão/confirmação explícita pelo candidato.

---

## Stack Tecnológica

| Camada | Tecnologia |
| :--- | :--- |
| **Backend** | Java 21 + Spring Boot 3.5 |
| **Frontend** | React 19 + TypeScript + Vite 7 |
| **Banco de Dados** | PostgreSQL 16 + Spring Data JPA / Hibernate |
| **Migrações** | Flyway (`V1__init_schema.sql`) |
| **Extração de Currículo** | Apache PDFBox 3 (PDF) + Apache POI 5 (DOCX) |
| **Estilização** | Tailwind CSS 4 + Lucide Icons |
| **Testes** | JUnit 5 + Mockito + Spring MockMvc (Backend) / Vitest + Testing Library (Frontend) |

---

## Pré-requisitos

- **Java 21** (`java -version`)
- **Apache Maven 3.8+** (`mvn -version`)
- **Node.js 20+ / 24+** (`node -v`)
- **PostgreSQL 16** (via Docker Compose ou instalação local)

---

## Configuração e Execução Local

### 1. Variáveis de Ambiente

Copie o arquivo `.env.example` para `.env` na raiz do projeto e ajuste os parâmetros conforme seu ambiente:

```bash
cp .env.example .env
```

> **Nota sobre o Provedor de IA (`LlmClient`):**
> - Caso `LLM_API_KEY` seja informada no `.env`, o sistema realizará chamadas estruturadas JSON ao endpoint compatível configurado em `LLM_API_URL` (ex.: Gemini OpenAI-compatible API ou OpenAI).
> - Caso `LLM_API_KEY` esteja em branco, o backend utiliza automaticamente o analisador determinístico local (`DeterministicResumeAnalyzer`), extraindo dados e evidências literais diretamente do texto do currículo sem inventar informações.

### 2. Banco de Dados PostgreSQL

Suba o PostgreSQL com Docker Compose:

```bash
docker compose up -d
```

*(Alternativamente, se estiver usando PostgreSQL 16 local na porta configurada em `DB_PORT`, as migrações Flyway criarão automaticamente as tabelas `users`, `resumes`, `candidate_profiles` e `profile_evidences` na inicialização do backend).*

### 3. Executar o Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080` e o Swagger UI em `http://localhost:8080/swagger-ui.html`.

### 4. Executar o Frontend (React + Vite)

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

A interface web estará disponível em `http://localhost:5173`.

---

## Executando os Testes Automatizados

### Testes do Backend (JUnit 5 + Integração PostgreSQL/Flyway)

```bash
mvn -f backend/pom.xml test
```

### Testes do Frontend (Vitest + Testing Library)

```bash
npm --prefix frontend run test
```

---

## Documentação Adicional

- [Arquitetura do Sistema](docs/architecture.md)
- [Modelagem do Banco de Dados](docs/database.md)
- [Contratos da API REST](docs/api.md)
# CandidAUTO
