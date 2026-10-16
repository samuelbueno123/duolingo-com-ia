package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record AdminNotificationResponseDTO(
    Long id,
    String type,
    String title,
    String message,
    boolean read,
    String referenceType,
    Long referenceId,
    LocalDateTime createdAt
) {
}
