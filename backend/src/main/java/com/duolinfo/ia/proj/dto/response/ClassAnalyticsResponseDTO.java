package com.duolinfo.ia.proj.dto.response;

import java.util.List;
import java.util.Map;

public record ClassAnalyticsResponseDTO(
    Long classId,
    String className,
    int totalStudents,
    int activeStudentsPastWeek,
    double averageAccuracyPercent,
    int totalSubmissions,
    int pendingGradingCount,
    Map<String, Long> cefrLevelDistribution,
    List<StudentRiskDTO> studentsNeedingAttention
) {}
