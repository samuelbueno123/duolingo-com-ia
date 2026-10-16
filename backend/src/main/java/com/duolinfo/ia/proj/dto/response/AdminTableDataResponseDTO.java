package com.duolinfo.ia.proj.dto.response;

import java.util.List;
import java.util.Map;

public record AdminTableDataResponseDTO(
    String tableName,
    String label,
    long totalCount,
    List<String> primaryKeys,
    List<AdminTableColumnResponseDTO> columns,
    List<Map<String, Object>> rows,
    boolean readOnly
) {
}
