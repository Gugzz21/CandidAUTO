package com.jobassistant.matching.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobMatchAnalysisDto(
        UUID id,
        UUID jobId,
        int compatibilityScore,
        ScoreBreakdownDto scoreBreakdown,
        List<String> positivePoints,
        List<String> attentionPoints,
        List<String> matchedRequirements,
        List<String> missingRequirements,
        String aiSummary,
        Instant analyzedAt
) {
}
