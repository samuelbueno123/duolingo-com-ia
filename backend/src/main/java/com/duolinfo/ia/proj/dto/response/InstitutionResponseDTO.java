package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma instituição")
public record InstitutionResponseDTO(

    Long id,
    String name,
    String code,
    String email,
    String phone,
    String website,
    String city,
    String state,
    String country,
    String address,
    String description,
    Boolean active,

    @Schema(
        example = "2026-01-01T10:00:00"
    )
    LocalDateTime createdAt,

    @Schema(
        example = "2026-01-05T15:30:00"
    )
    LocalDateTime updatedAt
) {
}