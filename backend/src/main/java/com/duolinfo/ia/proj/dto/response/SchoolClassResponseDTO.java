package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma turma")
public record SchoolClassResponseDTO(

    Long id,

    String name,

    String schoolYear,

    String shift,

    String gradeLevel,

    Long schoolId,

    Boolean active,

    LocalDateTime createdAt,

    LocalDateTime updatedAt
) {
}