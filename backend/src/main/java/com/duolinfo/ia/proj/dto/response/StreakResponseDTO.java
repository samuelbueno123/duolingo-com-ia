package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDate;

public record StreakResponseDTO(
    int currentStreak,
    int longestStreak,
    LocalDate lastActiveDate,
    int streakFreezes,
    int todayXpEarned,
    int dailyXpGoal,
    boolean goalReached
) {}
