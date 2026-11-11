package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponseDTO(
    Long id,
    LocalDateTime timestamp,
    String category,
    String severity,
    String action,
    String actorEmail,
    String actorRole,
    String targetEntity,
    String targetId,
    String result,
    String clientIp,
    String correlationId,
    String details
) {
}
