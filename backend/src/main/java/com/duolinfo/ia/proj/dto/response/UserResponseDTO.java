package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados públicos retornados de um usuário")
public record UserResponseDTO(

    @Schema(description = "ID interno do usuário", example = "1")
    Long id,

    @Schema(description = "Nome completo do usuário", example = "João Pedro da Silva")
    String name,

    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    String email,

    @Schema(description = "Google ID do usuário", example = "109876543210987654321")
    String googleId,

    @Schema(description = "URL da foto de perfil")
    String profilePicture,

    @Schema(description = "Sexo do usuário (MASCULINO ou FEMININO)", example = "MASCULINO")
    String gender,

    @Schema(description = "Indica se é o administrador geral supremo (admin@duolingo.com)")
    boolean superAdmin,

    @Schema(description = "Indica se é administrador de uma instituição")
    boolean institutionAdmin,

    @Schema(description = "Indica se é o primeiro administrador fundador da instituição")
    boolean primaryInstitutionAdmin,

    @Schema(description = "ID da instituição associada")
    Long institutionId,

    @Schema(description = "Nome da instituição associada")
    String institutionName
) {
    public UserResponseDTO(
            Long id,
            String name,
            String email,
            String googleId,
            String profilePicture) {
        this(
            id,
            name,
            email,
            googleId,
            profilePicture,
            "MASCULINO",
            email != null && "admin@duolingo.com".equalsIgnoreCase(email.trim()),
            false,
            false,
            null,
            null
        );
    }
}