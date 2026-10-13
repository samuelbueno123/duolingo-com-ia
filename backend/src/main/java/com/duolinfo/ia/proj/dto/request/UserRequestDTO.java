package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para criação ou atualização de usuário")
public record UserRequestDTO(

    @Schema(
        description = "Nome completo do usuário",
        example = "João Pedro da Silva"
    )
    @NotBlank(message = "O nome é obrigatório.")
    String name,

    @Schema(
        description = "Email do usuário",
        example = "joao@exemplo.com"
    )
    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    String email,

    @Schema(
        description = "ID do usuário no Google.",
        example = "109876543210987654321"
    )
    String googleId,

    @Schema(
        description = "URL ou Data URL da foto de perfil",
        example = "data:image/png;base64,..."
    )
    String profilePicture,

    @Schema(
        description = "Senha atual do usuário (necessária ao alterar a senha)",
        example = "SenhaAtual123!"
    )
    String currentPassword,

    @Schema(
        description = "Nova senha do usuário",
        example = "MinhaNovaSenha123!"
    )
    String password
) {
}
