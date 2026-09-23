package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para login com email e senha")
public class PasswordLoginRequest {

    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "Email inválido.")
    @Schema(
        description = "Email do usuário",
        example = "usuario@exemplo.com"
    )
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Schema(
        description = "Senha do usuário",
        example = "MinhaSenha123!"
    )
    private String password;

    public PasswordLoginRequest() {
    }

    public PasswordLoginRequest(
            String email,
            String password) {

        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}