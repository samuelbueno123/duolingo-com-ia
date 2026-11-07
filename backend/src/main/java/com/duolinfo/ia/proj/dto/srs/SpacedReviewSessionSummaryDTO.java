package com.duolinfo.ia.proj.dto.srs;

public record SpacedReviewSessionSummaryDTO(
    long totalCards,
    long dueToday,
    long completedToday,
    int totalXpEarned,
    double retentionRate,
    int masteredCount,
    int learningCount
) {
}
