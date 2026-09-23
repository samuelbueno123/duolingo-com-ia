package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de uma turma")
public record SchoolClassRequestDTO(

    @NotBlank
    @Schema(example = "1º Ano A")
    String name,

    @Schema(
        description = "Ano letivo",
        example = "2026"
    )
    String schoolYear,

    @Schema(
        description = "Turno da turma",
        example = "MANHÃ"
    )
    String shift,

    @Schema(
        description = "Série ou nível",
        example = "1º Ano"
    )
    String gradeLevel,

    @Schema(
        description = "ID da escola. Usado principalmente na atualização.",
        example = "1"
    )
    Long schoolId,

    @Schema(example = "true")
    Boolean active
) {
}