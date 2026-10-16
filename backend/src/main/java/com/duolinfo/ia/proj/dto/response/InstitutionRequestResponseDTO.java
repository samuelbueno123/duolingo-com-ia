package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

public record InstitutionRequestResponseDTO(
    Long id,
    String requestedName,
    String requesterName,
    String requesterEmail,
    String city,
    String state,
    String message,
    String status,
    LocalDateTime createdAt
) {
}
