package com.duolinfo.ia.proj.dto.response;

public record LearningRankingEntryResponseDTO(
    int rank,
    String studentName,
    String className,
    int xpEarned,
    int activitiesCompleted,
    int correctAnswers,
    int questionCount,
    String profilePicture,
    String schoolName,
    Long entityId,
    double accuracyPercent
) {
    public LearningRankingEntryResponseDTO(
        int rank,
        String studentName,
        String className,
        int xpEarned,
        int activitiesCompleted,
        int correctAnswers,
        int questionCount
    ) {
        this(
            rank,
            studentName,
            className,
            xpEarned,
            activitiesCompleted,
            correctAnswers,
            questionCount,
            null,
            null,
            null,
            questionCount > 0 ? Math.round((correctAnswers * 1000.0) / questionCount) / 10.0 : 0.0
        );
    }

    public LearningRankingEntryResponseDTO(
        int rank,
        String studentName,
        String className,
        int xpEarned,
        int activitiesCompleted,
        int correctAnswers,
        int questionCount,
        String profilePicture
    ) {
        this(
            rank,
            studentName,
            className,
            xpEarned,
            activitiesCompleted,
            correctAnswers,
            questionCount,
            profilePicture,
            null,
            null,
            questionCount > 0 ? Math.round((correctAnswers * 1000.0) / questionCount) / 10.0 : 0.0
        );
    }
}
