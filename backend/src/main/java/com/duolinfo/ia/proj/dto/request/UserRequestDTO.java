package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação ou atualização de usuário")
public record UserRequestDTO(

    @Schema(
        description = "Nome completo do usuário",
        example = "João Pedro da Silva"
    )
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    String name,

    @Schema(
        description = "Email do usuário",
        example = "joao@exemplo.com"
    )
    @Email(message = "Email inválido.")
    @Size(max = 254, message = "O email pode ter no máximo 254 caracteres.")
    String email,

    @Schema(
        description = "ID do usuário no Google.",
        example = "109876543210987654321"
    )
    String googleId,

    @Schema(
        description = "URL HTTP(S) ou Data URL de uma foto JPG/PNG de até 5 MB",
        example = "data:image/png;base64,..."
    )
    @Size(max = 7_000_000, message = "A foto de perfil excede o tamanho permitido.")
    String profilePicture,

    @Schema(
        description = "Senha atual do usuário (necessária ao alterar a senha)",
        example = "SenhaAtual123!"
    )
    @Size(max = 72, message = "A senha atual pode ter no máximo 72 caracteres.")
    String currentPassword,

    @Schema(
        description = "Nova senha do usuário",
        example = "MinhaNovaSenha123!"
    )
    @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres.")
    String password
) {
}
