package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record AchievementResponseDTO(
    Long id,
    String code,
    String title,
    String description,
    String category,
    String iconName,
    Integer displayOrder,
    boolean earned,
    LocalDateTime earnedAt,
    boolean grantedByAdmin
) {
}
