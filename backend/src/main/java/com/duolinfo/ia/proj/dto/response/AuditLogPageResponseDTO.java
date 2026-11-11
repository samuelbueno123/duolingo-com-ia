package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record AuditLogPageResponseDTO(
    List<AuditLogResponseDTO> content,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {
}
