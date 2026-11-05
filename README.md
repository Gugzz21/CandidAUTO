# CandidAUTO — Plataforma Inteligente de Busca e Candidatura a Vagas com IA

Aplicação web completa desenvolvida sob a arquitetura **Personal First, SaaS Ready** que atua como um **agente inteligente de recrutamento e candidaturas**: analisa currículos (PDF e DOCX) com rastreamento de evidências sem alucinação, descobre oportunidades reais em plataformas de emprego, calcula compatibilidade de `0 a 100%`, prepara o preenchimento inteligente de formulários com Cofre de Respostas Confirmadas e exige aprovação explícita do candidato antes de qualquer envio.

---

## Stack Tecnológica

| Camada | Tecnologia |
| :--- | :--- |
| **Backend** | Java 21 + Spring Boot 3.5 |
| **Frontend** | React 19 + TypeScript + Vite 7 |
| **Banco de Dados** | PostgreSQL 16 + Spring Data JPA / Hibernate |
| **Migrações** | Flyway (`V1` a `V4`) |
| **Extração de Currículo** | Apache PDFBox 3 (PDF) + Apache POI 5 (DOCX) |
| **Estilização** | Tailwind CSS 4 + Lucide Icons |
| **Testes** | JUnit 5 + Mockito + Spring MockMvc (Backend) / Vitest + Testing Library (Frontend) |

---

## Funcionalidades Implementadas (Etapas 1 a 6)

1. **Etapa 1 — Currículo e Perfil Estruturado com Evidências:** Upload de PDF/DOCX, extração local, estruturação por IA sem inventar qualificações, editor manual completo e confirmação explícita do perfil.
2. **Etapa 2 — Integração com Plataformas de Emprego Reais:** Conectores oficiais para **Greenhouse**, **Lever**, **Gupy / Páginas de Carreiras (Schema.org JSON-LD)** e importação assistida por URL/descrição para **LinkedIn, Indeed, Glassdoor, Programathor, Revelo e InfoJobs**, com deduplicação SHA-256 (`fingerprint_hash`).
3. **Etapa 3 — Motor de Compatibilidade (0 a 100%) e Painel de Oportunidades:** Avaliação multicritério explicável (pontos positivos, pontos de atenção, requisitos atendidos e não atendidos) sem eliminar vagas por requisitos desejáveis, acompanhada de 9 filtros avançados.
4. **Etapa 4 — Preenchimento Inteligente e Cofre do Candidato (`candidate_qa_vault`):** Autopreenchimento de formulários exclusivamente com dados confirmados e detecção automática de perguntas adicionais ausentes (`missingQuestions`).
5. **Etapa 5 — Tela de Revisão Final e Aprovação Explícita:** Botões **CANDIDATAR-SE** (exige confirmação humana explícita, gera recibo auditável e registra data/hora) e **CANCELAR** (interrompe sem enviar dados, mantendo rascunho ou descartando).
6. **Etapa 6 — Acompanhamento (11 Status), Indicadores e Monitoramento Automático:** Gestão completa do processo seletivo pelos 11 status oficiais, 6 indicadores no Dashboard Principal e configuração de busca automática por cargos desejados.

---

## Como Executar Localmente

### 1. Banco de Dados PostgreSQL 16
```bash
./infra/start-db.sh
```
*(Ou via Docker Compose com `docker compose up -d`).*

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

- **Backend (15 testes JUnit 5 + Integração PostgreSQL/Flyway):**
  ```bash
  mvn -f backend/pom.xml test
  ```
- **Frontend (8 testes Vitest + Testing Library):**
  ```bash
  npm --prefix frontend run test
  ```

---

## Documentação Adicional

- [Arquitetura do Sistema](docs/architecture.md)
- [Modelagem do Banco de Dados](docs/database.md)
- [Contratos da API REST](docs/api.md)
