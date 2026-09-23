package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Atribuição de professor a uma turma")
public record ClassTeacherAssignmentRequestDTO(

    @NotNull
    @Schema(
        description = "ID da turma",
        example = "10"
    )
    Long schoolClassId,

    @NotNull
    @Schema(
        description = "ID do professor",
        example = "5"
    )
    Long teacherId,

    @Schema(
        description = "Data de início",
        example = "2026-02-01"
    )
    LocalDate startDate,

    @Schema(
        description = "Motivo da atribuição",
        example = "Professor titular"
    )
    String reason
) {
}