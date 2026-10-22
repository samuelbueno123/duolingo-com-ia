package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDTO(
    @NotBlank(message = "Informe o seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254, message = "O e-mail pode ter no máximo 254 caracteres.")
    String email
) {
}
