package com.duolinfo.ia.proj.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para alteração do estado de uma matrícula")
public record EnrollmentActionDTO(

    @Schema(
        description = "Data da operação",
        example = "2026-05-20"
    )
    LocalDate date,

    @Schema(
        description = "Observações",
        example = "Solicitação dos responsáveis"
    )
    String notes
) {
}