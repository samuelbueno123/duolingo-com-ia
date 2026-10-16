package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record AdminTableMetadataResponseDTO(
    String tableName,
    String label,
    long rowCount,
    List<String> primaryKeys,
    List<AdminTableColumnResponseDTO> columns,
    boolean readOnly
) {
}
