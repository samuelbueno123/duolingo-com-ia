package com.duolinfo.ia.proj.dto.response;

public record AdminTableColumnResponseDTO(
    String name,
    String dataType,
    String udtName,
    boolean nullable,
    boolean isPrimaryKey,
    boolean isAutoIncrement,
    String defaultValue
) {
}
