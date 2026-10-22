package com.duolinfo.ia.proj.dto.response;

public record StudentProgressSummaryResponseDTO(
    int totalXp,
    int todayXp,
    int dailyGoalMinutes,
    int dailyGoalXp,
    int currentStreakDays,
    int bestStreakDays,
    int completedExercisesCount,
    int totalSubmissionsCount,
    boolean dailyGoalReached,
    String calculationCriteria
) {
}
