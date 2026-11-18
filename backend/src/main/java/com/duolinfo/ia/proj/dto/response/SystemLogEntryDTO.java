package com.duolinfo.ia.proj.dto.response;

public record SystemLogEntryDTO(
    long lineNumber,
    String timestamp,
    String level,
    String thread,
    String correlationId,
    String logger,
    String message,
    String rawLine
) {
}
