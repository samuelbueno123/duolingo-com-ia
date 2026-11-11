package com.duolinfo.ia.proj.dto.response;

public record StudentRiskDTO(
    Long studentId,
    String name,
    String email,
    int totalSubmissions,
    double averageScore,
    String riskReason
) {}
