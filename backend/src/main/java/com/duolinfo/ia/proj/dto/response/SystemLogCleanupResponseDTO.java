package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;

public record SystemLogCleanupResponseDTO(
    int deletedFiles,
    long freedBytes,
    int retentionDays,
    Instant executedAt
) {
}
