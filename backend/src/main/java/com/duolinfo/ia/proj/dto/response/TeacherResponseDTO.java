package com.duolinfo.ia.proj.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um professor")
public record TeacherResponseDTO(

    @Schema(example = "5")
    Long id,

    @Schema(example = "Carlos Mendes")
    String name,

    @Schema(example = "carlos@escola.com")
    String email,

    @Schema(example = "123456789")
    String googleId,

    @Schema(example = "1")
    Long institutionId,

    @Schema(example = "[\"Inglês\", \"Espanhol\"]")
    List<String> taughtLanguages,

    @Schema(example = "[\"Conversação\", \"Gramática\"]")
    List<String> specializationAreas,

    String bibliography
) {
}