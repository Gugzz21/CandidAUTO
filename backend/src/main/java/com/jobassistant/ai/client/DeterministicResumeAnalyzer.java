package com.jobassistant.ai.client;

import com.jobassistant.ai.dto.AiStructuredProfileResponse;
import com.jobassistant.profile.dto.CertificationItemDto;
import com.jobassistant.profile.dto.ContactInfoDto;
import com.jobassistant.profile.dto.EducationItemDto;
import com.jobassistant.profile.dto.ExperienceItemDto;
import com.jobassistant.profile.dto.LanguageItemDto;
import com.jobassistant.profile.dto.ProjectItemDto;
import com.jobassistant.profile.dto.SkillItemDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analisador determinístico baseado em evidências literais do texto do currículo.
 * Garante extração auditável e livre de alucinação quando nenhuma chave externa de LLM está configurada
 * ou como extrator de evidências verificáveis.
 */
@Component
public class DeterministicResumeAnalyzer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?9?\\d{4}[\\s\\-]?\\d{4}");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_%/]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB_PATTERN = Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[a-zA-Z0-9\\-_%/]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PORTFOLIO_PATTERN = Pattern.compile("(?:https?://)(?!(?:www\\.)?(?:linkedin|github)\\.com)[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "((?:\\d{2}/)?(?:19|20)\\d{2}|(?:Jan|Fev|Mar|Abr|Mai|Jun|Jul|Ago|Set|Out|Nov|Dez|Feb|Apr|May|Aug|Sep|Oct|Dec)[a-zç./]*\\s*(?:de\\s*)?(?:19|20)\\d{2})\\s*(?:-|–|—|a|até|to)\\s*((?:\\d{2}/)?(?:19|20)\\d{2}|Atual|Presente|Present|Atualmente|Current|(?:Jan|Fev|Mar|Abr|Mai|Jun|Jul|Ago|Set|Out|Nov|Dez|Feb|Apr|May|Aug|Sep|Oct|Dec)[a-zç./]*\\s*(?:de\\s*)?(?:19|20)\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> KNOWN_TECH_SKILLS = new LinkedHashMap<>();

    static {
        KNOWN_TECH_SKILLS.put("Java", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("Spring Boot", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Spring Security", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Spring Data", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Hibernate", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("JPA", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("TypeScript", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("JavaScript", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("React", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Next.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Node.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("NestJS", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Express", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Angular", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Vue.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Python", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("Django", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("FastAPI", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Flask", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("C#", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put(".NET", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("PHP", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("Laravel", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Go", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("Kotlin", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("PostgreSQL", "TOOL");
        KNOWN_TECH_SKILLS.put("MySQL", "TOOL");
        KNOWN_TECH_SKILLS.put("MongoDB", "TOOL");
        KNOWN_TECH_SKILLS.put("Redis", "TOOL");
        KNOWN_TECH_SKILLS.put("Oracle", "TOOL");
        KNOWN_TECH_SKILLS.put("SQL Server", "TOOL");
        KNOWN_TECH_SKILLS.put("Docker", "TOOL");
        KNOWN_TECH_SKILLS.put("Kubernetes", "TOOL");
        KNOWN_TECH_SKILLS.put("AWS", "TOOL");
        KNOWN_TECH_SKILLS.put("Azure", "TOOL");
        KNOWN_TECH_SKILLS.put("GCP", "TOOL");
        KNOWN_TECH_SKILLS.put("Git", "TOOL");
        KNOWN_TECH_SKILLS.put("GitHub", "TOOL");
        KNOWN_TECH_SKILLS.put("GitLab", "TOOL");
        KNOWN_TECH_SKILLS.put("Linux", "TOOL");
        KNOWN_TECH_SKILLS.put("Maven", "TOOL");
        KNOWN_TECH_SKILLS.put("Gradle", "TOOL");
        KNOWN_TECH_SKILLS.put("Tailwind CSS", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Bootstrap", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("HTML", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("CSS", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("SQL", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("REST API", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("APIs REST", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Microsserviços", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Microservices", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("GraphQL", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Kafka", "TOOL");
        KNOWN_TECH_SKILLS.put("RabbitMQ", "TOOL");
        KNOWN_TECH_SKILLS.put("JUnit", "TOOL");
        KNOWN_TECH_SKILLS.put("Mockito", "TOOL");
        KNOWN_TECH_SKILLS.put("Playwright", "TOOL");
        KNOWN_TECH_SKILLS.put("Cypress", "TOOL");
        KNOWN_TECH_SKILLS.put("CI/CD", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("TDD", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Clean Architecture", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("SOLID", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Scrum", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Kanban", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Trabalho em Equipe", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Comunicação", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Resolução de Problemas", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Proatividade", "SOFT_SKILL");
    }

    public AiStructuredProfileResponse analyze(String resumeText) {
        List<String> lines = resumeText.lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        // 1. Nome Completo (ignora títulos como "Currículo" ou linhas com e-mail/URL)
        String fullName = "";
        String fullNameEvidence = "";
        for (int i = 0; i < Math.min(lines.size(), 5); i++) {
            String candidate = lines.get(i);
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (candidate.length() >= 3 && candidate.length() <= 80
                    && !candidate.contains("@")
                    && !lower.contains("http")
                    && !lower.contains("linkedin.com")
                    && !lower.contains("github.com")
                    && !lower.equals("currículo")
                    && !lower.equals("curriculo")
                    && !lower.equals("curriculum vitae")
                    && !candidate.matches(".*\\d{4}.*")) {
                fullName = candidate;
                fullNameEvidence = candidate;
                break;
            }
        }

        // 2. Cargo / Headline
        String headline = "";
        String headlineEvidence = "";
        for (int i = 0; i < Math.min(lines.size(), 10); i++) {
            String line = lines.get(i);
            if (line.equals(fullName)) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("desenvolvedor") || lower.contains("developer") || lower.contains("engenheiro")
                    || lower.contains("engineer") || lower.contains("analista") || lower.contains("full stack")
                    || lower.contains("fullstack") || lower.contains("backend") || lower.contains("back-end")
                    || lower.contains("frontend") || lower.contains("front-end") || lower.contains("software")
                    || lower.contains("programador") || lower.contains("arquiteto") || lower.contains("devops")
                    || lower.contains("dados") || lower.contains("qa")) {
                headline = line;
                headlineEvidence = line;
                break;
            }
        }

        // 3. Senioridade
        String seniority = "";
        String seniorityEvidence = "";
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("sênior") || lower.contains("senior")) {
                seniority = "Sênior";
                seniorityEvidence = line;
                break;
            } else if (lower.contains("pleno") || lower.contains("mid-level")) {
                seniority = "Pleno";
                seniorityEvidence = line;
                break;
            } else if (lower.contains("júnior") || lower.contains("junior") || lower.matches(".*\\bjr\\b.*")) {
                seniority = "Júnior";
                seniorityEvidence = line;
                break;
            } else if (lower.contains("estágio") || lower.contains("estagiário") || lower.contains("estagiario") || lower.contains("intern")) {
                seniority = "Estágio";
                seniorityEvidence = line;
                break;
            }
        }

        ContactInfoDto contactInfo = extractContactInfo(resumeText, lines);

        // 4. Resumo Profissional (suporta múltiplos parágrafos ou primeiro parágrafo descritivo)
        String summary = "";
        String summaryEvidence = "";
        for (int i = 0; i < lines.size(); i++) {
            String lower = lines.get(i).toLowerCase(Locale.ROOT).replaceAll("[:\\-–—]+$", "").trim();
            if (lower.equals("resumo") || lower.equals("resumo profissional") || lower.equals("sobre")
                    || lower.equals("sobre mim") || lower.equals("perfil") || lower.equals("perfil profissional")
                    || lower.equals("objetivo") || lower.equals("objetivo profissional") || lower.equals("summary")) {
                StringBuilder sb = new StringBuilder();
                String firstEv = "";
                for (int j = i + 1; j < Math.min(lines.size(), i + 5); j++) {
                    String nextLine = lines.get(j);
                    if (isSectionHeader(nextLine)) break;
                    if (firstEv.isEmpty()) firstEv = nextLine;
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(nextLine);
                }
                summary = sb.toString().trim();
                summaryEvidence = firstEv;
                break;
            }
        }
        // Fallback: se não houver cabeçalho "Resumo", procura uma linha descritiva longa no topo do currículo
        if (summary.isEmpty()) {
            for (int i = 1; i < Math.min(lines.size(), 12); i++) {
                String line = lines.get(i);
                if (line.length() >= 55 && !line.contains("@") && !DATE_RANGE_PATTERN.matcher(line).find()) {
                    summary = line;
                    summaryEvidence = line;
                    break;
                }
            }
        }

        List<SkillItemDto> skills = extractSkills(lines);
        List<ExperienceItemDto> experiences = extractExperiences(lines);
        List<EducationItemDto> education = extractEducation(lines);
        List<LanguageItemDto> languages = extractLanguages(lines);
        List<CertificationItemDto> certifications = extractCertifications(lines);
        List<ProjectItemDto> projects = extractProjects(lines);

        List<String> targetRoles = new ArrayList<>();
        if (!headline.isBlank()) {
            targetRoles.add(headline);
        }

        return new AiStructuredProfileResponse(
                fullName,
                fullNameEvidence,
                headline,
                headlineEvidence,
                seniority,
                seniorityEvidence,
                summary,
                summaryEvidence,
                contactInfo,
                experiences,
                education,
                skills,
                languages,
                certifications,
                projects,
                targetRoles
        );
    }

    private boolean isSectionHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT).replaceAll("[:\\-–—]+$", "").trim();
        return Set.of(
                "experiência", "experiencia", "experiências", "experiencias", "experiência profissional",
                "histórico profissional", "formação", "formacao", "formação acadêmica", "educação", "educacao",
                "habilidades", "competências", "competencias", "tecnologias", "skills", "idiomas",
                "certificações", "certificacoes", "cursos", "projetos", "projects", "education", "experience"
        ).contains(lower);
    }

    private ContactInfoDto extractContactInfo(String fullText, List<String> lines) {
        String email = findFirstMatch(EMAIL_PATTERN, fullText);
        String phone = findFirstMatch(PHONE_PATTERN, fullText);
        String linkedin = findFirstMatch(LINKEDIN_PATTERN, fullText);
        String github = findFirstMatch(GITHUB_PATTERN, fullText);
        String portfolio = findFirstMatch(PORTFOLIO_PATTERN, fullText);
        String location = "";

        for (String line : lines.subList(0, Math.min(lines.size(), 12))) {
            if (line.matches(".*\\b(SP|RJ|MG|PR|SC|RS|BA|PE|CE|DF|GO|ES|AM|PA|MT|MS|PB|RN|AL|SE|PI|MA|RO|TO|AC|AP|RR|Brasil|Brazil|Remoto|São Paulo|Rio de Janeiro|Belo Horizonte|Curitiba|Porto Alegre|Florianópolis|Campinas|Recife|Salvador|Brasília|Fortaleza)\\b.*")) {
                location = line;
                break;
            }
        }

        return new ContactInfoDto(email, phone, location, linkedin, github, portfolio);
    }

    private List<SkillItemDto> extractSkills(List<String> lines) {
        List<SkillItemDto> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Map.Entry<String, String> entry : KNOWN_TECH_SKILLS.entrySet()) {
            String skillName = entry.getKey();
            String category = entry.getValue();
            Pattern pattern = Pattern.compile("(?<![a-zA-Z0-9])" + Pattern.quote(skillName) + "(?![a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);

            for (String line : lines) {
                if (pattern.matcher(line).find() && seen.add(skillName.toLowerCase(Locale.ROOT))) {
                    result.add(new SkillItemDto(skillName, category, "Extraído do currículo", line));
                    break;
                }
            }
        }
        return result;
    }

    private List<ExperienceItemDto> extractExperiences(List<String> lines) {
        List<ExperienceItemDto> experiences = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = DATE_RANGE_PATTERN.matcher(line);
            if (matcher.find()) {
                String startDate = matcher.group(1).trim();
                String endDate = matcher.group(2).trim();
                boolean current = endDate.toLowerCase(Locale.ROOT).matches(".*(atual|presente|present|atualmente|current).*");

                String headerPart = line.substring(0, matcher.start()).replaceAll("[|\\-–—•,()]+$", "").trim();
                String tailPart = line.substring(matcher.end()).replaceAll("^[|\\-–—•,()]+", "").trim();

                String role = headerPart;
                String company = tailPart;

                if (headerPart.contains(" - ")) {
                    String[] parts = headerPart.split(" - ", 2);
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (headerPart.contains(" | ")) {
                    String[] parts = headerPart.split(" \\| ", 2);
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (headerPart.contains(" na ") || headerPart.contains(" at ")) {
                    String[] parts = headerPart.split(" (?i)(na|at) ", 2);
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (headerPart.isBlank() && i > 0) {
                    String prevLine = lines.get(i - 1);
                    if (prevLine.contains(" - ")) {
                        String[] parts = prevLine.split(" - ", 2);
                        role = parts[0].trim();
                        company = parts[1].trim();
                    } else if (prevLine.contains(" | ")) {
                        String[] parts = prevLine.split(" \\| ", 2);
                        role = parts[0].trim();
                        company = parts[1].trim();
                    } else {
                        role = prevLine;
                        if (i > 1 && !isSectionHeader(lines.get(i - 2))) {
                            company = lines.get(i - 2);
                        }
                    }
                }

                String checkText = (role + " " + company + " " + line).toLowerCase(Locale.ROOT);
                if (checkText.contains("bacharel") || checkText.contains("tecnólogo") || checkText.contains("tecnologo")
                        || checkText.contains("universidade") || checkText.contains("faculdade")
                        || checkText.contains("ensino médio") || checkText.contains("graduação")
                        || checkText.contains("pós-graduação") || checkText.contains("mestrado")) {
                    continue;
                }

                // Coletar até 3 linhas de descrição da experiência
                StringBuilder descBuilder = new StringBuilder();
                for (int j = i + 1; j < Math.min(lines.size(), i + 4); j++) {
                    String nextLine = lines.get(j);
                    if (isSectionHeader(nextLine) || DATE_RANGE_PATTERN.matcher(nextLine).find()) {
                        break;
                    }
                    if (!descBuilder.isEmpty()) descBuilder.append(" ");
                    descBuilder.append(nextLine);
                }
                String description = descBuilder.toString().trim();

                List<String> techs = new ArrayList<>();
                String combinedForTech = (line + " " + description).toLowerCase(Locale.ROOT);
                for (String tech : KNOWN_TECH_SKILLS.keySet()) {
                    if (combinedForTech.contains(tech.toLowerCase(Locale.ROOT))) {
                        techs.add(tech);
                    }
                }

                experiences.add(new ExperienceItemDto(
                        company.isBlank() ? "Empresa informada no currículo" : company,
                        role.isBlank() ? "Cargo informado no currículo" : role,
                        startDate,
                        endDate,
                        current,
                        description,
                        techs,
                        line
                ));
            }
        }
        return experiences;
    }

    private List<EducationItemDto> extractEducation(List<String> lines) {
        List<EducationItemDto> education = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("bacharel") || lower.contains("tecnólogo") || lower.contains("tecnologo")
                    || lower.contains("engenharia") || lower.contains("ciência da computação")
                    || lower.contains("ciencia da computacao") || lower.contains("análise e desenvolvimento")
                    || lower.contains("analise e desenvolvimento") || lower.contains("sistemas de informação")
                    || lower.contains("sistemas de informacao") || lower.contains("universidade")
                    || lower.contains("faculdade") || lower.contains("graduação") || lower.contains("graduacao")
                    || lower.contains("pós-graduação") || lower.contains("mba") || lower.contains("fatec")
                    || lower.contains("usp") || lower.contains("unicamp") || lower.contains("puc")
                    || lower.contains("ufmg") || lower.contains("ufrj") || lower.contains("utfpr")) {
                if (isSectionHeader(line)) continue;

                String degree = line;
                String institution = line;
                if (line.contains(" - ")) {
                    String[] parts = line.split(" - ", 2);
                    degree = parts[0].trim();
                    institution = parts[1].trim();
                } else if (line.contains(" | ")) {
                    String[] parts = line.split(" \\| ", 2);
                    degree = parts[0].trim();
                    institution = parts[1].trim();
                } else if (i + 1 < lines.size() && !isSectionHeader(lines.get(i + 1))) {
                    institution = lines.get(i + 1);
                }

                education.add(new EducationItemDto(
                        institution,
                        degree,
                        "Tecnologia / Computação",
                        "",
                        "",
                        "Identificado no currículo",
                        line
                ));
            }
        }
        return education;
    }

    private List<LanguageItemDto> extractLanguages(List<String> lines) {
        List<LanguageItemDto> languages = new ArrayList<>();
        Map<String, String> targetLangs = new LinkedHashMap<>();
        targetLangs.put("inglês", "Inglês");
        targetLangs.put("ingles", "Inglês");
        targetLangs.put("english", "Inglês");
        targetLangs.put("espanhol", "Espanhol");
        targetLangs.put("spanish", "Espanhol");
        targetLangs.put("português", "Português");
        targetLangs.put("portugues", "Português");
        targetLangs.put("francês", "Francês");
        targetLangs.put("alemão", "Alemão");
        targetLangs.put("italiano", "Italiano");

        Set<String> added = new LinkedHashSet<>();

        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : targetLangs.entrySet()) {
                if (lower.contains(entry.getKey()) && added.add(entry.getValue())) {
                    String level = "Informado no currículo";
                    if (lower.contains("fluente") || lower.contains("avançado") || lower.contains("avancado") || lower.contains("fluent") || lower.contains("c1") || lower.contains("c2")) {
                        level = "Avançado / Fluente";
                    } else if (lower.contains("intermediário") || lower.contains("intermediario") || lower.contains("intermediate") || lower.contains("b1") || lower.contains("b2")) {
                        level = "Intermediário";
                    } else if (lower.contains("básico") || lower.contains("basico") || lower.contains("técnico") || lower.contains("tecnico") || lower.contains("a1") || lower.contains("a2")) {
                        level = "Básico / Técnico";
                    } else if (lower.contains("nativo") || lower.contains("native")) {
                        level = "Nativo";
                    }
                    languages.add(new LanguageItemDto(entry.getValue(), level, line));
                }
            }
        }
        return languages;
    }

    private List<CertificationItemDto> extractCertifications(List<String> lines) {
        List<CertificationItemDto> certs = new ArrayList<>();
        boolean inCertSection = false;
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (isSectionHeader(line)) {
                inCertSection = lower.contains("certifica") || lower.contains("curso");
                continue;
            }
            if (inCertSection || lower.contains("certifica") || lower.contains("aws certified")
                    || lower.contains("oracle certified") || lower.contains("azure") && lower.contains("az-")
                    || lower.contains("scrum master") || lower.startsWith("curso:")) {
                if (line.length() > 4) {
                    certs.add(new CertificationItemDto(line, "Extraído do currículo", "", line));
                }
            }
        }
        return certs;
    }

    private List<ProjectItemDto> extractProjects(List<String> lines) {
        List<ProjectItemDto> projects = new ArrayList<>();
        boolean inProjectSection = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (isSectionHeader(line)) {
                inProjectSection = lower.contains("projeto") || lower.contains("project");
                continue;
            }
            if (lower.startsWith("projeto:") || lower.startsWith("project:") || (inProjectSection && line.length() > 5)) {
                String desc = (i + 1 < lines.size() && !isSectionHeader(lines.get(i + 1))) ? lines.get(i + 1) : line;
                List<String> techs = new ArrayList<>();
                String combined = (line + " " + desc).toLowerCase(Locale.ROOT);
                for (String tech : KNOWN_TECH_SKILLS.keySet()) {
                    if (combined.contains(tech.toLowerCase(Locale.ROOT))) {
                        techs.add(tech);
                    }
                }
                projects.add(new ProjectItemDto(line, desc, techs, "", line));
                if (inProjectSection && i + 1 < lines.size() && desc.equals(lines.get(i + 1))) {
                    i++; // Avançar linha já consumida como descrição
                }
            }
        }
        return projects;
    }

    private String findFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : "";
    }
}
