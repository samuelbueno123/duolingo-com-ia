package com.duolinfo.ia.proj.dto.response;

import java.util.List;

public record SystemLogFileListResponseDTO(
    int retentionDays,
    String logDirectory,
    int totalFiles,
    long totalSizeBytes,
    List<SystemLogFileInfoDTO> files
) {
}
