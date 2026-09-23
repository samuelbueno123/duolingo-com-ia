package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para matrícula de um estudante")
public record EnrollmentRequestDTO(

    @NotNull
    @Schema(
        description = "ID do estudante",
        example = "15"
    )
    Long studentId,

    @NotNull
    @Schema(
        description = "ID da turma",
        example = "8"
    )
    Long schoolClassId,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(
        description = "Data da matrícula",
        example = "2026-02-01"
    )
    LocalDate enrollmentDate,

    @Schema(
        description = "Observações da matrícula",
        example = "Matrícula regular"
    )
    String notes
) {
}