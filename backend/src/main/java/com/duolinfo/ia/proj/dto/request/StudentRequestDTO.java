package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de um estudante")
public record StudentRequestDTO(

    @Schema(
        description = "Nome completo do estudante",
        example = "Maria Oliveira"
    )
    @NotBlank(message = "O nome é obrigatório.")
    String name,

    @Schema(
        description = "Email do estudante",
        example = "maria@exemplo.com"
    )
    @Email(message = "Email inválido.")
    String email,

    @Schema(
        description = "ID da conta Google",
        example = "12345678901234567890"
    )
    String googleId,

    @Schema(
        description = "Idiomas de preferência do estudante",
        example = "[\"Inglês\", \"Espanhol\"]"
    )
    List<String> languages
) {
}