package com.duolinfo.ia.proj.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um estudante")
public record StudentResponseDTO(

    @Schema(example = "15")
    Long id,

    @Schema(example = "Maria Oliveira")
    String name,

    @Schema(example = "maria@exemplo.com")
    String email,

    @Schema(example = "123456789")
    String googleId,

    @Schema(example = "[\"Inglês\", \"Espanhol\"]")
    List<String> languages
) {
}