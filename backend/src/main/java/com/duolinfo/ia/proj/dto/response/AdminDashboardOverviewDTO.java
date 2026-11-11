package com.duolinfo.ia.proj.dto.response;

import java.util.List;
import java.util.Map;

public record AdminDashboardOverviewDTO(
    long totalUsers,
    long totalStudents,
    long totalTeachers,
    long pendingTeachers,
    long totalInstitutions,
    long totalClasses,
    long totalExercises,
    long totalSubmissions,
    long totalEnrollments,
    long totalCatalogLanguages,
    Map<String, Long> profileDistribution,
    Map<String, Long> userStatus,
    List<Map<String, Object>> topLanguages,
    List<Map<String, Object>> topSpecializations,
    Map<String, Long> auditSummary,
    List<AuditLogResponseDTO> recentActivities
) {
}
