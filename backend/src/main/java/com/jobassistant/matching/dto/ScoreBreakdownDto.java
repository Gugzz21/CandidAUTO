package com.jobassistant.matching.dto;

public record ScoreBreakdownDto(
        int techAndRequirementsScore,
        int seniorityScore,
        int modalityAndLocationScore,
        int educationAndLanguagesScore,
        int desirableBonusScore
) {
    public static ScoreBreakdownDto empty() {
        return new ScoreBreakdownDto(0, 0, 0, 0, 0);
    }
}
