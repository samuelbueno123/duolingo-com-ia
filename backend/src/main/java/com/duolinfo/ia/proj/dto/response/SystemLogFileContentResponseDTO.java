package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;
import java.util.List;

public record SystemLogFileContentResponseDTO(
    String fileName,
    String channel,
    long sizeBytes,
    Instant lastModified,
    int retentionDays,
    long totalLines,
    long matchedLines,
    long infoCount,
    long warnCount,
    long errorCount,
    int page,
    int limit,
    List<SystemLogEntryDTO> entries
) {
}
