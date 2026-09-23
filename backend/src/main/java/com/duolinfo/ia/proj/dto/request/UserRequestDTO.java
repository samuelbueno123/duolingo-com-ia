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
        description = "ID do usuário no Google. Pode ficar vazio para usuários que utilizam senha.",
        example = "109876543210987654321"
    )
    String googleId,

    @Schema(
        description = "URL da foto de perfil",
        example = "https://lh3.googleusercontent.com/..."
    )
    String profilePicture,

    @Schema(
        description = "Senha em texto puro. Será armazenada como hash e nunca retornada pela API.",
        example = "MinhaSenha123!"
    )
    String password
) {
}