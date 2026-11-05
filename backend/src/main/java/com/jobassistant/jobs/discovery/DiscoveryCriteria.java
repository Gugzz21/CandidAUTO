package com.jobassistant.jobs.discovery;

import java.util.List;

/**
 * Critérios consolidados de busca automática de vagas de um usuário,
 * unindo preferências de automação (SearchConfig) e dados do perfil confirmado.
 */
public record DiscoveryCriteria(
        List<String> desiredRoles,
        List<String> desiredModalities,
        List<String> desiredSeniorities,
        List<String> candidateSkills
) {
    public DiscoveryCriteria {
        desiredRoles = desiredRoles != null ? desiredRoles.stream().filter(s -> s != null && !s.isBlank()).toList() : List.of();
        desiredModalities = desiredModalities != null ? desiredModalities.stream().filter(s -> s != null && !s.isBlank()).toList() : List.of();
        desiredSeniorities = desiredSeniorities != null ? desiredSeniorities.stream().filter(s -> s != null && !s.isBlank()).toList() : List.of();
        candidateSkills = candidateSkills != null ? candidateSkills.stream().filter(s -> s != null && !s.isBlank()).toList() : List.of();
    }

    public boolean hasRoleConstraints() {
        return !desiredRoles.isEmpty() || !candidateSkills.isEmpty();
    }
}
