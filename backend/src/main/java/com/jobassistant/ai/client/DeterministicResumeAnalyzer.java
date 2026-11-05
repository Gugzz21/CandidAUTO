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
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("((?:\\d{2}/)?(?:19|20)\\d{2}|(?:Jan|Fev|Mar|Abr|Mai|Jun|Jul|Ago|Set|Out|Nov|Dez)[a-zç]*\\s*(?:de\\s*)?(?:19|20)\\d{2})\\s*(?:-|–|—|a|até|to)\\s*((?:\\d{2}/)?(?:19|20)\\d{2}|Atual|Presente|Present|Atualmente|(?:Jan|Fev|Mar|Abr|Mai|Jun|Jul|Ago|Set|Out|Nov|Dez)[a-zç]*\\s*(?:de\\s*)?(?:19|20)\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> KNOWN_TECH_SKILLS = new LinkedHashMap<>();

    static {
        KNOWN_TECH_SKILLS.put("Java", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("Spring Boot", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Spring Security", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Spring Data", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Hibernate", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("TypeScript", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("JavaScript", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("React", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Next.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Node.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("NestJS", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Angular", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Vue.js", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Python", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("C#", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put(".NET", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("Go", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("PostgreSQL", "TOOL");
        KNOWN_TECH_SKILLS.put("MySQL", "TOOL");
        KNOWN_TECH_SKILLS.put("MongoDB", "TOOL");
        KNOWN_TECH_SKILLS.put("Redis", "TOOL");
        KNOWN_TECH_SKILLS.put("Docker", "TOOL");
        KNOWN_TECH_SKILLS.put("Kubernetes", "TOOL");
        KNOWN_TECH_SKILLS.put("AWS", "TOOL");
        KNOWN_TECH_SKILLS.put("Azure", "TOOL");
        KNOWN_TECH_SKILLS.put("GCP", "TOOL");
        KNOWN_TECH_SKILLS.put("Git", "TOOL");
        KNOWN_TECH_SKILLS.put("Tailwind CSS", "FRAMEWORK");
        KNOWN_TECH_SKILLS.put("HTML", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("CSS", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("SQL", "LANGUAGE_TECH");
        KNOWN_TECH_SKILLS.put("REST API", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("GraphQL", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Kafka", "TOOL");
        KNOWN_TECH_SKILLS.put("RabbitMQ", "TOOL");
        KNOWN_TECH_SKILLS.put("JUnit", "TOOL");
        KNOWN_TECH_SKILLS.put("Playwright", "TOOL");
        KNOWN_TECH_SKILLS.put("CI/CD", "HARD_SKILL");
        KNOWN_TECH_SKILLS.put("Scrum", "SOFT_SKILL");
        KNOWN_TECH_SKILLS.put("Kanban", "SOFT_SKILL");
    }

    public AiStructuredProfileResponse analyze(String resumeText) {
        List<String> lines = resumeText.lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        String fullName = "";
        String fullNameEvidence = "";
        if (!lines.isEmpty()) {
            String firstLine = lines.get(0);
            if (firstLine.length() <= 80 && !firstLine.contains("@")) {
                fullName = firstLine;
                fullNameEvidence = firstLine;
            }
        }

        String headline = "";
        String headlineEvidence = "";
        for (int i = 1; i < Math.min(lines.size(), 6); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("desenvolvedor") || lower.contains("developer") || lower.contains("engenheiro")
                    || lower.contains("engineer") || lower.contains("analista") || lower.contains("full stack")
                    || lower.contains("backend") || lower.contains("back-end") || lower.contains("frontend")
                    || lower.contains("front-end") || lower.contains("software")) {
                headline = line;
                headlineEvidence = line;
                break;
            }
        }

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
            } else if (lower.contains("júnior") || lower.contains("junior") || lower.contains("jr")) {
                seniority = "Júnior";
                seniorityEvidence = line;
                break;
            } else if (lower.contains("estágio") || lower.contains("estagiário") || lower.contains("intern")) {
                seniority = "Estágio";
                seniorityEvidence = line;
                break;
            }
        }

        ContactInfoDto contactInfo = extractContactInfo(resumeText, lines);
        String summary = "";
        String summaryEvidence = "";

        for (int i = 0; i < lines.size(); i++) {
            String lower = lines.get(i).toLowerCase(Locale.ROOT);
            if ((lower.equals("resumo") || lower.equals("resumo profissional") || lower.equals("sobre")
                    || lower.equals("perfil") || lower.equals("summary")) && i + 1 < lines.size()) {
                summary = lines.get(i + 1);
                summaryEvidence = lines.get(i + 1);
                break;
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

    private ContactInfoDto extractContactInfo(String fullText, List<String> lines) {
        String email = findFirstMatch(EMAIL_PATTERN, fullText);
        String phone = findFirstMatch(PHONE_PATTERN, fullText);
        String linkedin = findFirstMatch(LINKEDIN_PATTERN, fullText);
        String github = findFirstMatch(GITHUB_PATTERN, fullText);
        String location = "";

        for (String line : lines.subList(0, Math.min(lines.size(), 10))) {
            if (line.matches(".*\\b(SP|RJ|MG|PR|SC|RS|BA|PE|CE|DF|GO|Brasil|Brazil|Remoto)\\b.*")) {
                location = line;
                break;
            }
        }

        return new ContactInfoDto(email, phone, location, linkedin, github, "");
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
                boolean current = endDate.toLowerCase(Locale.ROOT).matches(".*(atual|presente|present|atualmente).*");

                String headerPart = line.substring(0, matcher.start()).replaceAll("[|\\-–—]+$", "").trim();
                String role = headerPart;
                String company = "";
                if (headerPart.contains(" - ")) {
                    String[] parts = headerPart.split(" - ", 2);
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (headerPart.contains(" | ")) {
                    String[] parts = headerPart.split(" \\| ", 2);
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (headerPart.isBlank() && i > 0) {
                    role = lines.get(i - 1);
                }

                // Evitar confundir formação acadêmica com experiência profissional
                String lowerRole = role.toLowerCase(Locale.ROOT);
                if (lowerRole.contains("bacharel") || lowerRole.contains("tecnólogo") || lowerRole.contains("universidade")
                        || lowerRole.contains("faculdade") || lowerRole.contains("ensino")) {
                    continue;
                }

                String description = (i + 1 < lines.size()) ? lines.get(i + 1) : "";
                List<String> techs = new ArrayList<>();
                for (String tech : KNOWN_TECH_SKILLS.keySet()) {
                    if (description.toLowerCase(Locale.ROOT).contains(tech.toLowerCase(Locale.ROOT))) {
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
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("bacharel") || lower.contains("tecnólogo") || lower.contains("engenharia")
                    || lower.contains("ciência da computação") || lower.contains("análise e desenvolvimento")
                    || lower.contains("sistemas de informação") || lower.contains("universidade")
                    || lower.contains("faculdade") || lower.contains("graduação") || lower.contains("pós-graduação")) {
                education.add(new EducationItemDto(
                        line,
                        line,
                        "",
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
        Map<String, String> targetLangs = Map.of(
                "inglês", "Inglês",
                "english", "Inglês",
                "espanhol", "Espanhol",
                "português", "Português",
                "francês", "Francês"
        );
        Set<String> added = new LinkedHashSet<>();

        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : targetLangs.entrySet()) {
                if (lower.contains(entry.getKey()) && added.add(entry.getValue())) {
                    String level = "Informado no currículo";
                    if (lower.contains("fluente") || lower.contains("avançado")) {
                        level = "Avançado / Fluente";
                    } else if (lower.contains("intermediário")) {
                        level = "Intermediário";
                    } else if (lower.contains("básico") || lower.contains("técnico")) {
                        level = "Básico / Técnico";
                    } else if (lower.contains("nativo")) {
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
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("certifica") || lower.contains("aws certified") || lower.contains("oracle certified")
                    || lower.contains("scrum master") || lower.contains("curso:")) {
                certs.add(new CertificationItemDto(line, "Extraído do currículo", "", line));
            }
        }
        return certs;
    }

    private List<ProjectItemDto> extractProjects(List<String> lines) {
        List<ProjectItemDto> projects = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("projeto:") || lower.startsWith("project:")) {
                String desc = (i + 1 < lines.size()) ? lines.get(i + 1) : line;
                projects.add(new ProjectItemDto(line, desc, List.of(), "", line));
            }
        }
        return projects;
    }

    private String findFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : "";
    }
}
