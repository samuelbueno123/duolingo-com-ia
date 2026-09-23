package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de uma instituição")
public record InstitutionRequestDTO(

    @NotBlank
    @Schema(example = "Colégio XPTO")
    String name,

    @NotBlank
    @Schema(example = "XPTO001")
    String code,

    @NotBlank
    @Email
    @Schema(example = "contato@xpto.com.br")
    String email,

    @Schema(example = "21999999999")
    String phone,

    @Schema(example = "https://www.xpto.com.br")
    String website,

    @Schema(example = "Rio de Janeiro")
    String city,

    @Schema(example = "RJ")
    String state,

    @Schema(example = "Brasil")
    String country,

    @Schema(example = "Rua das Flores, 100")
    String address,

    @Schema(example = "Instituição de ensino técnico.")
    String description,

    @Schema(example = "true")
    Boolean active
) {
}