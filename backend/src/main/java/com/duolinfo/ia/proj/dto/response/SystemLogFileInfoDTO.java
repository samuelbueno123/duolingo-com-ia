package com.duolinfo.ia.proj.dto.response;

import java.time.Instant;

public record SystemLogFileInfoDTO(
    String fileName,
    String channel,
    String label,
    String date,
    boolean compressed,
    long sizeBytes,
    Instant lastModified
) {
}
