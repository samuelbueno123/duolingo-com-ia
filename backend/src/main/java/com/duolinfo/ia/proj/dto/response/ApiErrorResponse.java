package com.duolinfo.ia.proj.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de erro da API")
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    String path,

    @Schema(example = "c8b417f7-b8fa-4a25-831e-43644ec8dc36")
    String correlationId,

    @Schema(description = "Erros de validação campo a campo")
    Map<String, String> validationErrors
) {
    public ApiErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path) {
        this(timestamp, status, error, message, path, null, null);
    }

    public ApiErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            String correlationId) {
        this(timestamp, status, error, message, path, correlationId, null);
    }
}