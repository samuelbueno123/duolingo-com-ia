package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserRequestDTO(
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    String name,

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    @Size(max = 254, message = "O email pode ter no máximo 254 caracteres.")
    String email,

    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres.")
    String password,

    boolean isAdmin,

    String gender
) {
}
