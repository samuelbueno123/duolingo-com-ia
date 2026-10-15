package com.duolinfo.ia.proj.dto.response;

public record LearningRankingEntryResponseDTO(
    int rank,
    String studentName,
    String className,
    int xpEarned,
    int activitiesCompleted,
    int correctAnswers,
    int questionCount
) {
}
