package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados de um professor")
public record TeacherRequestDTO(

    @Schema(
        description = "Nome completo do professor",
        example = "Carlos Mendes"
    )
    @NotBlank(message = "O nome é obrigatório.")
    String name,

    @Schema(
        description = "Email do professor",
        example = "carlos@escola.com"
    )
    @Email(message = "Email inválido.")
    String email,

    @Schema(
        description = "Google ID do professor",
        example = "123456789"
    )
    String googleId,

    @Schema(
        description = "ID da instituição do professor",
        example = "1"
    )
    @NotNull(message = "A instituição é obrigatória.")
    Long institutionId,

    @Schema(
        description = "Idiomas lecionados",
        example = "[\"Inglês\", \"Espanhol\"]"
    )
    List<String> taughtLanguages,

    @Schema(
        description = "Áreas de especialização",
        example = "[\"Gramática\", \"Conversação\"]"
    )
    List<String> specializationAreas,

    @Schema(
        description = "Currículo ou biografia do professor",
        example = "Professor com 10 anos de experiência..."
    )
    String bibliography
) {
}