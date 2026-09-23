package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para encerramento de uma atribuição")
public record ClassTeacherAssignmentFinishDTO(

    @Schema(
        description = "Data de encerramento",
        example = "2026-05-31"
    )
    LocalDate endDate
) {
}