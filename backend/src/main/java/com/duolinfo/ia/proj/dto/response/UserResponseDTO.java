package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados públicos retornados de um usuário")
public record UserResponseDTO(

    @Schema(
        description = "ID interno do usuário",
        example = "1"
    )
    Long id,

    @Schema(
        description = "Nome completo do usuário",
        example = "João Pedro da Silva"
    )
    String name,

    @Schema(
        description = "Email do usuário",
        example = "joao@exemplo.com"
    )
    String email,

    @Schema(
        description = "Google ID do usuário",
        example = "109876543210987654321"
    )
    String googleId,

    @Schema(
        description = "URL da foto de perfil",
        example = "https://lh3.googleusercontent.com/..."
    )
    String profilePicture
) {
}