package com.jobassistant.jobs.service;

import com.jobassistant.jobs.connector.RawDiscoveredJob;
import com.jobassistant.jobs.domain.JobPostingEntity;
import com.jobassistant.jobs.domain.WorkModality;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobNormalizationService {

    private static final List<String> TECH_CATALOG = List.of(
            "Java", "Spring Boot", "Spring Security", "Hibernate", "JPA",
            "TypeScript", "JavaScript", "React", "Next.js", "Angular", "Vue.js",
            "Node.js", "NestJS", "Python", "Django", "FastAPI", "C#", ".NET", "Go",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Kafka", "RabbitMQ",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Terraform", "Git",
            "CI/CD", "REST API", "GraphQL", "Microsserviços", "Microservices",
            "JUnit", "Mockito", "Jest", "Vitest", "Playwright", "Cypress",
            "Tailwind CSS", "HTML", "CSS", "SQL", "Linux"
    );

    private static final List<String> BENEFIT_KEYWORDS = List.of(
            "Vale Refeição", "Vale Alimentação", "Plano de Saúde", "Plano Odontológico",
            "Gympass", "Wellhub", "Auxílio Home Office", "PLR", "Bônus",
            "Seguro de Vida", "Auxílio Educação", "Horário Flexível", "Day Off"
    );

    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "(?:R\\$|US\\$|USD|BRL)\\s?\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})?(?:\\s?(?:-|a|até|to)\\s?(?:R\\$|US\\$|USD|BRL)?\\s?\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})?)?",
            Pattern.CASE_INSENSITIVE
    );

    public JobPostingEntity normalizeToEntity(UUID userId, UUID sourceId, RawDiscoveredJob raw) {
        String cleanTitle = cleanSingleLine(raw.title());
        String cleanCompany = cleanSingleLine(raw.companyName());
        String cleanLocation = cleanSingleLine(raw.location());
        String cleanDescription = stripHtmlToFormattedText(raw.descriptionRaw());

        String fingerprint = computeFingerprint(cleanCompany, cleanTitle, cleanLocation);

        JobPostingEntity entity = new JobPostingEntity(
                UUID.randomUUID(),
                userId,
                sourceId,
                fingerprint,
                raw.externalId(),
                raw.platform(),
                cleanCompany.isBlank() ? "Empresa não informada" : cleanCompany,
                cleanTitle,
                cleanDescription,
                raw.originalUrl() != null ? raw.originalUrl().trim() : ""
        );

        entity.setLocation(cleanLocation);
        entity.setWorkModality(resolveWorkModality(raw.explicitModality(), cleanTitle, cleanLocation, cleanDescription));
        entity.setSeniority(resolveSeniority(raw.explicitSeniority(), cleanTitle, cleanDescription));
        entity.setSalaryRange(resolveSalary(raw.salaryRange(), cleanDescription));

        List<String> techs = extractTechnologies(cleanTitle + "\n" + cleanDescription);
        entity.setTechnologies(techs);

        RequirementsSplit split = extractRequirements(cleanDescription, techs);
        entity.setMandatoryRequirements(split.mandatory());
        entity.setDesirableRequirements(split.desirable());
        entity.setBenefits(extractBenefits(cleanDescription));
        entity.setApplicationQuestions(raw.applicationQuestions() != null ? raw.applicationQuestions() : List.of());
        entity.setPublishedAt(raw.publishedAt());
        entity.setActive(true);

        return entity;
    }

    public void updateExistingEntity(JobPostingEntity existing, RawDiscoveredJob raw) {
        String cleanDescription = stripHtmlToFormattedText(raw.descriptionRaw());
        existing.setDescriptionRaw(cleanDescription);
        existing.setLocation(cleanSingleLine(raw.location()));
        existing.setWorkModality(resolveWorkModality(raw.explicitModality(), existing.getTitle(), existing.getLocation(), cleanDescription));
        existing.setSeniority(resolveSeniority(raw.explicitSeniority(), existing.getTitle(), cleanDescription));
        existing.setSalaryRange(resolveSalary(raw.salaryRange(), cleanDescription));

        List<String> techs = extractTechnologies(existing.getTitle() + "\n" + cleanDescription);
        existing.setTechnologies(techs);
        RequirementsSplit split = extractRequirements(cleanDescription, techs);
        existing.setMandatoryRequirements(split.mandatory());
        existing.setDesirableRequirements(split.desirable());
        existing.setBenefits(extractBenefits(cleanDescription));
        if (raw.applicationQuestions() != null && !raw.applicationQuestions().isEmpty()) {
            existing.setApplicationQuestions(raw.applicationQuestions());
        }
        if (raw.originalUrl() != null && !raw.originalUrl().isBlank()) {
            existing.setOriginalUrl(raw.originalUrl().trim());
        }
        existing.setActive(true);
    }

    public String computeFingerprint(String companyName, String title, String location) {
        String canonical = normalizeKey(companyName) + "|" + normalizeKey(title) + "|" + normalizeKey(location);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    public String stripHtmlToFormattedText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("(?i)</h[1-6]>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private WorkModality resolveWorkModality(WorkModality explicit, String title, String location, String description) {
        if (explicit != null && explicit != WorkModality.NAO_INFORMADO) {
            return explicit;
        }
        String combined = (title + " " + location + " " + description).toLowerCase(Locale.ROOT);
        if (combined.contains("híbrido") || combined.contains("hibrido") || combined.contains("hybrid")) {
            return WorkModality.HIBRIDO;
        }
        if (combined.contains("remoto") || combined.contains("remote") || combined.contains("home office") || combined.contains("anywhere")) {
            return WorkModality.REMOTO;
        }
        if (combined.contains("presencial") || combined.contains("on-site") || combined.contains("onsite")) {
            return WorkModality.PRESENCIAL;
        }
        return WorkModality.NAO_INFORMADO;
    }

    private String resolveSeniority(String explicit, String title, String description) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        if (lowerTitle.contains("estágio") || lowerTitle.contains("estagiário") || lowerTitle.contains("intern")) {
            return "Estágio";
        }
        if (lowerTitle.contains("júnior") || lowerTitle.contains("junior") || lowerTitle.matches(".*\\bjr\\b.*")) {
            return "Júnior";
        }
        if (lowerTitle.contains("sênior") || lowerTitle.contains("senior") || lowerTitle.matches(".*\\bsr\\b.*") || lowerTitle.contains("staff") || lowerTitle.contains("principal")) {
            return "Sênior";
        }
        if (lowerTitle.contains("pleno") || lowerTitle.contains("mid") || lowerTitle.matches(".*\\bpl\\b.*")) {
            return "Pleno";
        }

        String lowerDesc = description.toLowerCase(Locale.ROOT);
        if (lowerDesc.contains("nível júnior") || lowerDesc.contains("vaga júnior")) return "Júnior";
        if (lowerDesc.contains("nível pleno") || lowerDesc.contains("vaga pleno")) return "Pleno";
        if (lowerDesc.contains("nível sênior") || lowerDesc.contains("vaga sênior")) return "Sênior";
        return "Pleno / Geral";
    }

    private String resolveSalary(String explicitSalary, String description) {
        if (explicitSalary != null && !explicitSalary.isBlank()) {
            return explicitSalary.trim();
        }
        Matcher matcher = SALARY_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private List<String> extractTechnologies(String text) {
        Set<String> found = new LinkedHashSet<>();
        for (String tech : TECH_CATALOG) {
            Pattern p = Pattern.compile("(?<![a-zA-Z0-9])" + Pattern.quote(tech) + "(?![a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
            if (p.matcher(text).find()) {
                found.add(tech);
            }
        }
        return new ArrayList<>(found);
    }

    private RequirementsSplit extractRequirements(String description, List<String> technologies) {
        List<String> mandatory = new ArrayList<>();
        List<String> desirable = new ArrayList<>();

        boolean inDesirableSection = false;
        boolean inRequirementsSection = false;

        for (String rawLine : description.lines().toList()) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);

            if (lower.contains("desejável") || lower.contains("desejaveis") || lower.contains("diferencial")
                    || lower.contains("diferenciais") || lower.contains("nice to have") || lower.contains("bonus points")) {
                inDesirableSection = true;
                inRequirementsSection = false;
                if (line.length() > 35 && (line.startsWith("•") || line.startsWith("-"))) {
                    desirable.add(cleanBullet(line));
                }
                continue;
            }

            if (lower.contains("requisitos") || lower.contains("qualificações") || lower.contains("o que esperamos")
                    || lower.contains("requirements") || lower.contains("what you need") || lower.contains("obrigatório")) {
                inRequirementsSection = true;
                inDesirableSection = false;
                if (line.length() > 35 && (line.startsWith("•") || line.startsWith("-"))) {
                    mandatory.add(cleanBullet(line));
                }
                continue;
            }

            if (lower.startsWith("benefícios") || lower.startsWith("benefits") || lower.startsWith("sobre a empresa")) {
                inRequirementsSection = false;
                inDesirableSection = false;
                continue;
            }

            boolean isBullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("*");
            if (isBullet || inRequirementsSection || inDesirableSection) {
                String cleaned = cleanBullet(line);
                if (cleaned.length() >= 8 && cleaned.length() <= 220) {
                    if (inDesirableSection || lower.contains("desejável") || lower.contains("diferencial") || lower.contains("nice to have")) {
                        desirable.add(cleaned);
                    } else if (inRequirementsSection || lower.contains("experiência com") || lower.contains("conhecimento em")
                            || lower.contains("experience with") || lower.contains("domínio de")) {
                        mandatory.add(cleaned);
                    }
                }
            }
        }

        if (mandatory.isEmpty() && !technologies.isEmpty()) {
            for (String tech : technologies) {
                mandatory.add("Conhecimento / experiência em " + tech);
            }
        }

        return new RequirementsSplit(
                mandatory.stream().distinct().limit(12).toList(),
                desirable.stream().distinct().limit(10).toList()
        );
    }

    private List<String> extractBenefits(String description) {
        List<String> found = new ArrayList<>();
        String lower = description.toLowerCase(Locale.ROOT);
        for (String benefit : BENEFIT_KEYWORDS) {
            if (lower.contains(benefit.toLowerCase(Locale.ROOT))) {
                found.add(benefit);
            }
        }
        return found;
    }

    private String cleanBullet(String line) {
        return line.replaceFirst("^[•\\-*]+\\s*", "").trim();
    }

    private String cleanSingleLine(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    private String normalizeKey(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9à-ú]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private record RequirementsSplit(List<String> mandatory, List<String> desirable) {
    }
}
