package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
    @Size(max = 254)
    String email,

    @NotBlank(message = "Informe o token de recuperação.")
    @Size(min = 10, max = 1024, message = "Token de recuperação inválido.")
    String token,

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres.")
    String newPassword
) {
    public ResetPasswordRequestDTO(String token, String newPassword) {
        this(null, token, newPassword);
    }
}
