package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credencial recebida do Google")
public class GoogleLoginRequest {

    @NotBlank(message = "A credencial do Google é obrigatória.")
    @Schema(
        description = "Credential retornada pelo Google Identity Services",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
    )
    private String credential;

    public GoogleLoginRequest() {
    }

    public GoogleLoginRequest(String credential) {
        this.credential = credential;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}