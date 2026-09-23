package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de erro da API")
public record ApiErrorResponse(

    @Schema(example = "2026-09-22T20:00:00")
    LocalDateTime timestamp,

    @Schema(example = "404")
    int status,

    @Schema(example = "Not Found")
    String error,

    @Schema(example = "Escola não encontrada: 10")
    String message,

    @Schema(example = "/api/schools/10")
    String path
) {
}