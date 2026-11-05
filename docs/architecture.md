# Arquitetura do Sistema — CandidAUTO (Etapas 1 a 6)

## Monólito Modular (Personal First, SaaS Ready)

O **CandidAUTO** utiliza uma arquitetura de **Monólito Modular** em Java 21 + Spring Boot 3.5 no backend e React 19 + TypeScript + Vite 7 no frontend:

- **`profile` (`com.jobassistant.profile`):** Gerenciamento do perfil profissional estruturado, evidências rastreáveis, edição manual e confirmação explícita do candidato.
- **`resume` (`com.jobassistant.resume`):** Upload seguro, validação binária (magic bytes), armazenamento privado (`storage_key` interno UUID), extração de texto com Apache PDFBox (PDF) e Apache POI (DOCX), e máquina de estados do processamento.
- **`ai` (`com.jobassistant.ai`):** Interface desacoplada `LlmClient`, construção de prompts com delimitação de dados não confiáveis (`<untrusted_resume_text>`), cliente HTTP compatível com JSON Schema e verificador de evidências contra o texto original do currículo.
- **`jobs` (`com.jobassistant.jobs`):** Camada de conectores independentes (`GreenhouseJobConnector`, `LeverJobConnector`, `CareerPageJsonLdConnector`), importação assistida, normalização e deduplicação por `fingerprint_hash`.
- **`matching` (`com.jobassistant.matching`):** Motor de compatibilidade de `0 a 100%` (`JobMatchingEngineService`) com explicação detalhada (pontos positivos, pontos de atenção, requisitos atendidos/não atendidos) e painel filtrável (`OpportunitiesController`).
- **`applications` (`com.jobassistant.applications`):** Preenchimento inteligente (`SmartApplicationService`), Cofre de Respostas Confirmadas (`candidate_qa_vault`), detecção de perguntas ausentes, revisão obrigatória (`CANDIDATAR-SE` / `CANCELAR`) e acompanhamento pelos 11 status.
- **`automation` (`com.jobassistant.automation`):** Configuração de cargos procurados, execução de monitoramento periódico sem repetição de vagas já analisadas/candidatadas e cálculo dos 6 indicadores do Dashboard Principal (`AutomatedJobMonitoringService`).
- **`security` (`com.jobassistant.security`):** Configuração de segurança stateless, CORS e resolução de escopo por `user_id` (`CurrentUserProvider`), garantindo isolamento completo de dados entre usuários.
- **`shared` (`com.jobassistant.shared`):** Tratamento global e seguro de exceções (`GlobalExceptionHandler`).
