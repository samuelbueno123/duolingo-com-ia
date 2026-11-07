package com.duolinfo.ia.proj.dto.pronunciation;

public record PronunciationStatsDTO(
    long totalAttempts,
    double averageScore,
    long masteredChallengesCount,
    String strongestCategory,
    String needsImprovementCategory
) {}
