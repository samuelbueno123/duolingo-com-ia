package com.duolinfo.ia.proj.dto.response;

import java.util.List;
import java.util.Map;

public record AdminSystemPerformanceDTO(
    String overallStatus,
    long timestampEpochMs,
    long uptimeSeconds,
    Map<String, Object> httpMetrics,
    Map<String, Object> jvmMetrics,
    Map<String, Object> databasePool,
    Map<String, Object> realtimeMetrics,
    List<Map<String, Object>> telemetryHistory,
    List<Map<String, Object>> languageBreakdown,
    Map<String, Long> profileDistribution,
    Map<String, Long> auditSeverityDistribution
) {
}
