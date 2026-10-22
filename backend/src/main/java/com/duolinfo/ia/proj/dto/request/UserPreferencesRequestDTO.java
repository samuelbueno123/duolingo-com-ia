package com.duolinfo.ia.proj.dto.request;

public record UserPreferencesRequestDTO(
    String uiLanguage,
    String themeMode,
    Integer dailyGoalMinutes,
    Boolean notifDailyReminders,
    Boolean notifAchievements,
    Boolean notifRanking,
    Boolean notifClassUpdates
) {
}
