package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Posição de um aluno em um ranking de pontuação")
public record RankResponseDTO(
        @Schema(description = "Posição no ranking, começando em 0", example = "0")
        Long position,
        @Schema(example = "15")
        Long studentId,
        @Schema(example = "Maria Oliveira")
        String studentName,
        @Schema(description = "Pontuação usada para ordenar o ranking", example = "1200")
        Long score) {
}

