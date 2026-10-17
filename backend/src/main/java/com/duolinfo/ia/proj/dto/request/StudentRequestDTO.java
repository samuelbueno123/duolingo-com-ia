package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    String email,

    @Schema(
        description = "Senha do estudante",
        example = "senha123"
    )
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.")
    String password,

    @Schema(
        description = "ID da conta Google",
        example = "12345678901234567890"
    )
    String googleId,

    @Schema(
        description = "Sexo do estudante (MASCULINO ou FEMININO)",
        example = "FEMININO"
    )
    String gender,

    @Schema(
        description = "ID da instituição do estudante (opcional)",
        example = "1"
    )
    Long institutionId,

    @Schema(
        description = "Idiomas de preferência e proficiência do estudante"
    )
    List<StudentLanguageRequestDTO> languages
) {
}
