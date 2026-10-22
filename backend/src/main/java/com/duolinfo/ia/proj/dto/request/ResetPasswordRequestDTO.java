package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
    @NotBlank(message = "Informe o seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254)
    String email,

    @NotBlank(message = "Informe o código de recuperação.")
    @Size(min = 6, max = 64, message = "Código de recuperação inválido.")
    String token,

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 6, max = 72, message = "A nova senha deve ter entre 6 e 72 caracteres.")
    String newPassword
) {
}
