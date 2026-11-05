package com.jobassistant.jobs.discovery;

import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.domain.WorkModality;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Conecta os filtros de pesquisa do usuário (cargos, senioridades, modalidades e skills)
 * tanto à geração de consultas para APIs públicas quanto à filtragem semântica das vagas descobertas.
 */
@Component
public class JobRelevanceFilter {

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "da", "do", "das", "dos", "para", "em", "com", "e", "ou", "a", "o",
            "pessoa", "analista", "pleno", "junior", "senior", "jr", "pl", "sr",
            "estagio", "estagiario", "trainee", "nivel", "vaga", "remoto", "hibrido", "presencial"
    );

    private static final Map<String, List<String>> BILINGUAL_ROLE_SYNONYMS = Map.ofEntries(
            Map.entry("desenvolvedor", List.of("desenvolvedor", "desenvolvedora", "developer", "software engineer", "engenheiro de software", "programador", "swe")),
            Map.entry("developer", List.of("developer", "desenvolvedor", "desenvolvedora", "software engineer", "engenheiro de software")),
            Map.entry("engenheiro", List.of("engenheiro", "engenheira", "engineer", "desenvolvedor", "developer")),
            Map.entry("front-end", List.of("front-end", "frontend", "front end", "react", "angular", "vue", "ui engineer")),
            Map.entry("frontend", List.of("frontend", "front-end", "front end", "react", "angular", "vue")),
            Map.entry("back-end", List.of("back-end", "backend", "back end", "api", "java", "node", "python", "dotnet", ".net", "golang")),
            Map.entry("backend", List.of("backend", "back-end", "back end", "java", "spring", "node", "python")),
            Map.entry("full stack", List.of("full stack", "fullstack", "full-stack", "software engineer", "desenvolvedor", "developer")),
            Map.entry("fullstack", List.of("fullstack", "full stack", "full-stack", "software engineer", "desenvolvedor", "developer")),
            Map.entry("dados", List.of("dados", "data engineer", "data scientist", "data analyst", "analytics", "bi")),
            Map.entry("qa", List.of("qa", "quality assurance", "testes", "automacao de testes", "sdet", "tester")),
            Map.entry("devops", List.of("devops", "sre", "site reliability", "cloud engineer", "infraestrutura", "platform engineer")),
            Map.entry("mobile", List.of("mobile", "android", "ios", "react native", "flutter", "kotlin", "swift"))
    );

    private static final List<String> UNRELATED_NON_TECH_MARKERS = List.of(
            "account executive", "sales representative", "business development representative",
            "sdr", "bdr", "vendedor", "executiva de contas", "executivo de contas",
            "recruiter", "talent acquisition", "recrutador", "recursos humanos",
            "legal counsel", "advogado", "advogada", "juridico",
            "contador", "contabil", "tax analyst", "payroll", "folha de pagamento",
            "customer support", "atendimento ao cliente", "recepcionista", "motorista"
    );

    public List<String> buildPortugueseQueries(DiscoveryCriteria criteria) {
        Set<String> queries = new LinkedHashSet<>();
        for (String role : criteria.desiredRoles()) {
            String simplified = simplifyPortugueseRoleForQuery(role);
            if (!simplified.isBlank()) {
                queries.add(simplified);
            }
            if (queries.size() >= 4) {
                break;
            }
        }
        if (queries.isEmpty() && !criteria.candidateSkills().isEmpty()) {
            queries.add("Desenvolvedor " + criteria.candidateSkills().get(0));
        }
        if (queries.isEmpty()) {
            queries.add("Desenvolvedor Júnior");
        }
        return new ArrayList<>(queries);
    }

    public List<String> buildEnglishQueries(DiscoveryCriteria criteria) {
        Set<String> queries = new LinkedHashSet<>();
        for (String role : criteria.desiredRoles()) {
            String english = translateRoleToEnglishQuery(role);
            if (!english.isBlank()) {
                queries.add(english);
            }
            if (queries.size() >= 4) {
                break;
            }
        }
        if (queries.isEmpty() && !criteria.candidateSkills().isEmpty()) {
            queries.add(criteria.candidateSkills().get(0) + " Developer");
        }
        if (queries.isEmpty()) {
            queries.add("Software Developer");
        }
        return new ArrayList<>(queries);
    }

    public record RelevanceDecision(boolean relevant, String discardReason) {
    }

    public boolean isRelevant(RawDiscoveredJob job, DiscoveryCriteria criteria) {
        return evaluateRelevance(job, criteria).relevant();
    }

    public RelevanceDecision evaluateRelevance(RawDiscoveredJob job, DiscoveryCriteria criteria) {
        if (job == null || job.title() == null || job.title().isBlank()) {
            return new RelevanceDecision(false, "TITULO_VAZIO");
        }
        if (criteria == null || !criteria.hasRoleConstraints()) {
            if (!matchesSeniority(job.title(), criteria)) {
                return new RelevanceDecision(false, "SENIORIDADE_INCOMPATIVEL");
            }
            if (!matchesModality(job, criteria)) {
                return new RelevanceDecision(false, "MODALIDADE_INCOMPATIVEL");
            }
            return new RelevanceDecision(true, null);
        }

        String normTitle = normalizeText(job.title());
        String normDesc = normalizeText(job.descriptionRaw());
        String combinedRolesConfig = normalizeText(String.join(" ", criteria.desiredRoles()));

        // 1. Excluir áreas claramente alheias (Vendas, Jurídico, RH, Contabilidade), a menos que o usuário tenha pedido
        for (String marker : UNRELATED_NON_TECH_MARKERS) {
            if (containsPhrase(normTitle, marker) && !combinedRolesConfig.contains(marker)) {
                return new RelevanceDecision(false, "AREA_ALHEIA_AO_CARGO");
            }
        }

        // 2. Verificar compatibilidade de Senioridade no título
        if (!matchesSeniority(job.title(), criteria)) {
            return new RelevanceDecision(false, "SENIORIDADE_INCOMPATIVEL");
        }

        // 3. Verificar compatibilidade de Modalidade (quando explícita na vaga)
        if (!matchesModality(job, criteria)) {
            return new RelevanceDecision(false, "MODALIDADE_INCOMPATIVEL");
        }

        // 4. Verificar aderência semântica do Cargo / Especialidade / Tecnologias
        Set<String> acceptedTitleKeywords = extractTargetKeywords(criteria);
        for (String keyword : acceptedTitleKeywords) {
            if (containsPhrase(normTitle, keyword)) {
                return new RelevanceDecision(true, null);
            }
        }

        // Caso o título seja genérico de engenharia (ex.: "Software Engineer" ou "Pessoa Desenvolvedora"),
        // validar se alguma skill ou especialidade aparece no título ou na descrição
        boolean isGenericDevTitle = containsPhrase(normTitle, "software engineer")
                || containsPhrase(normTitle, "engenheiro de software")
                || containsPhrase(normTitle, "desenvolvedor")
                || containsPhrase(normTitle, "desenvolvedora")
                || containsPhrase(normTitle, "developer")
                || containsPhrase(normTitle, "programador");

        boolean userWantsDevRole = combinedRolesConfig.contains("desenvolvedor")
                || combinedRolesConfig.contains("developer")
                || combinedRolesConfig.contains("engenheiro")
                || combinedRolesConfig.contains("engineer")
                || combinedRolesConfig.contains("front")
                || combinedRolesConfig.contains("back")
                || combinedRolesConfig.contains("full");

        if (isGenericDevTitle && userWantsDevRole) {
            if (criteria.candidateSkills().isEmpty()) {
                return new RelevanceDecision(true, null);
            }
            for (String skill : criteria.candidateSkills()) {
                String normSkill = normalizeText(skill);
                if (!normSkill.isBlank() && (containsPhrase(normTitle, normSkill) || containsPhrase(normDesc, normSkill))) {
                    return new RelevanceDecision(true, null);
                }
            }
            return new RelevanceDecision(true, null);
        }

        return new RelevanceDecision(false, "CARGO_OU_TECNOLOGIA_NAO_ADERENTE");
    }

    private boolean matchesSeniority(String rawTitle, DiscoveryCriteria criteria) {
        if (criteria == null || criteria.desiredSeniorities().isEmpty()) {
            return true;
        }
        String normTitle = normalizeText(rawTitle);
        String wanted = normalizeText(String.join(" ", criteria.desiredSeniorities()));

        boolean wantsSenior = wanted.contains("senior") || wanted.contains("sr") || wanted.contains("especialista") || wanted.contains("lead");
        boolean wantsPleno = wanted.contains("pleno") || wanted.contains("mid") || wanted.contains("pl");

        // Bloquear cargos de liderança/sênior se o candidato busca apenas Júnior/Pleno/Estágio
        if (!wantsSenior) {
            if (containsWord(normTitle, "senior")
                    || containsWord(normTitle, "sr")
                    || containsWord(normTitle, "staff")
                    || containsWord(normTitle, "principal")
                    || containsWord(normTitle, "director")
                    || containsWord(normTitle, "diretor")
                    || containsWord(normTitle, "head")
                    || containsWord(normTitle, "manager")
                    || containsWord(normTitle, "gerente")
                    || containsWord(normTitle, "coordenador")
                    || containsWord(normTitle, "architect")
                    || containsWord(normTitle, "arquiteto")
                    || containsWord(normTitle, "especialista")
                    || containsPhrase(normTitle, "tech lead")) {
                return false;
            }
        }

        // Se o usuário pediu exclusivamente Júnior/Estágio (sem Pleno e sem Sênior), filtrar vagas explicitamente Pleno
        if (!wantsPleno && !wantsSenior) {
            if (containsWord(normTitle, "pleno") || containsPhrase(normTitle, "mid level") || containsPhrase(normTitle, "mid-level")) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesModality(RawDiscoveredJob job, DiscoveryCriteria criteria) {
        if (criteria == null || criteria.desiredModalities().isEmpty() || criteria.desiredModalities().size() >= 3) {
            return true;
        }
        WorkModality detected = inferModality(job);
        if (detected == null || detected == WorkModality.NAO_INFORMADO) {
            return true;
        }
        return criteria.desiredModalities().stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .anyMatch(m -> m.equals(detected.name()));
    }

    private WorkModality inferModality(RawDiscoveredJob job) {
        if (job.explicitModality() != null && job.explicitModality() != WorkModality.NAO_INFORMADO) {
            return job.explicitModality();
        }
        String text = normalizeText((job.title() != null ? job.title() : "") + " " + (job.location() != null ? job.location() : ""));
        if (text.contains("hibrido") || text.contains("hybrid")) {
            return WorkModality.HIBRIDO;
        }
        if (text.contains("remoto") || text.contains("remote") || text.contains("home office") || text.contains("anywhere")) {
            return WorkModality.REMOTO;
        }
        if (text.contains("presencial") || text.contains("on-site") || text.contains("onsite")) {
            return WorkModality.PRESENCIAL;
        }
        return WorkModality.NAO_INFORMADO;
    }

    private Set<String> extractTargetKeywords(DiscoveryCriteria criteria) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String role : criteria.desiredRoles()) {
            String normRole = normalizeText(role);
            for (Map.Entry<String, List<String>> entry : BILINGUAL_ROLE_SYNONYMS.entrySet()) {
                if (normRole.contains(entry.getKey())) {
                    for (String syn : entry.getValue()) {
                        keywords.add(normalizeText(syn));
                    }
                }
            }
            for (String token : normRole.split("\\s+")) {
                if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                    keywords.add(token);
                }
            }
        }
        for (String skill : criteria.candidateSkills()) {
            String normSkill = normalizeText(skill);
            if (normSkill.length() >= 2) {
                keywords.add(normSkill);
            }
        }
        return keywords;
    }

    private String simplifyPortugueseRoleForQuery(String role) {
        if (role == null) return "";
        String cleaned = role.trim()
                .replaceAll("(?i)\\b(júnior|junior|jr|pleno|pl|sênior|senior|sr|nível\\s+[ivx0-9]+)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? role.trim() : cleaned;
    }

    private String translateRoleToEnglishQuery(String role) {
        String norm = normalizeText(role);
        if (norm.contains("front-end") || norm.contains("frontend") || norm.contains("front end")) {
            return "Frontend Developer";
        }
        if (norm.contains("full stack") || norm.contains("fullstack") || norm.contains("full-stack")) {
            return "Full Stack Developer";
        }
        if (norm.contains("java")) {
            return "Java Developer";
        }
        if (norm.contains("python")) {
            return "Python Developer";
        }
        if (norm.contains("react")) {
            return "React Developer";
        }
        if (norm.contains("node")) {
            return "Node Developer";
        }
        if (norm.contains("back-end") || norm.contains("backend") || norm.contains("back end")) {
            return "Backend Developer";
        }
        if (norm.contains("dados") || norm.contains("data")) {
            return "Data Engineer";
        }
        if (norm.contains("qa") || norm.contains("teste")) {
            return "QA Engineer";
        }
        if (norm.contains("devops") || norm.contains("cloud")) {
            return "DevOps Engineer";
        }
        if (norm.contains("mobile") || norm.contains("android") || norm.contains("ios")) {
            return "Mobile Developer";
        }
        if (norm.contains("desenvolvedor") || norm.contains("programador") || norm.contains("engenheiro de software")) {
            return "Software Developer";
        }
        return simplifyPortugueseRoleForQuery(role);
    }

    public String normalizeText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.#+\\-/\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsPhrase(String text, String phrase) {
        if (text.isEmpty() || phrase.isEmpty()) {
            return false;
        }
        if (phrase.length() <= 3) {
            return containsWord(text, phrase);
        }
        return text.contains(phrase);
    }

    private boolean containsWord(String text, String word) {
        Pattern p = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(word) + "(?![a-z0-9])");
        return p.matcher(text).find();
    }
}
