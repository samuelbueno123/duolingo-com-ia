package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do usuário autenticado")
public record AuthenticatedUserResponseDTO(

    @Schema(
        description = "Indica se existe autenticação válida",
        example = "true"
    )
    boolean success,

    @Schema(
        description = "Indica se o usuário está autenticado",
        example = "true"
    )
    boolean authenticated,

    @Schema(
        description = "Dados públicos do usuário"
    )
    UserResponseDTO user,

    @Schema(
        description = "Tipo de perfil",
        example = "STUDENT"
    )
    String profileType
) {
}