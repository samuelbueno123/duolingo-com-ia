package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para transferência de turma")
public record EnrollmentTransferDTO(

    @NotNull
    @Schema(
        description = "Nova turma",
        example = "12"
    )
    Long newSchoolClassId,

    @Schema(
        description = "Data da transferência",
        example = "2026-06-01"
    )
    LocalDate transferDate,

    @Schema(
        description = "Motivo ou observação",
        example = "Mudança de turno"
    )
    String notes
) {
}