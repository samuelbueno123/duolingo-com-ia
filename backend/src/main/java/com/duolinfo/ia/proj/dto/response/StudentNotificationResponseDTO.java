package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record StudentNotificationResponseDTO(
    Long id,
    String title,
    String message,
    String type,
    boolean read,
    String actionUrl,
    LocalDateTime createdAt
) {}
