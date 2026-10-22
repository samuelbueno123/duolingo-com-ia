package com.duolinfo.ia.proj.dto.response;

public record UserPreferencesResponseDTO(
    String uiLanguage,
    String themeMode,
    int dailyGoalMinutes,
    int dailyGoalXp,
    boolean notifDailyReminders,
    boolean notifAchievements,
    boolean notifRanking,
    boolean notifClassUpdates,
    boolean externalEmailConfigured,
    String notificationChannelNotice
) {
}
