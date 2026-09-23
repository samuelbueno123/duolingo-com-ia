package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de autenticação")
public record AuthResponseDTO(

    @Schema(
        description = "Indica se a operação foi realizada com sucesso",
        example = "true"
    )
    boolean success,

    @Schema(
        description = "Mensagem da operação",
        example = "Login realizado com sucesso."
    )
    String message,

    @Schema(
        description = "Token JWT da aplicação",
        example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    String token,

    @Schema(
        description = "Dados públicos do usuário autenticado"
    )
    UserResponseDTO user,

    @Schema(
        description = "Tipo de perfil identificado",
        example = "TEACHER",
        allowableValues = {
            "USER",
            "STUDENT",
            "TEACHER"
        }
    )
    String profileType
) {
}