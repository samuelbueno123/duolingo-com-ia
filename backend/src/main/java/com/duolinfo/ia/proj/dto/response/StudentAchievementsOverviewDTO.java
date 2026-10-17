package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record StudentAchievementsOverviewDTO(
    Long studentId,
    String studentName,
    String studentEmail,
    String gender,
    String institutionName,
    List<AchievementResponseDTO> achievements
) {
}
