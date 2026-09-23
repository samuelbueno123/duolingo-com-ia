package com.duolinfo.ia.proj.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de uma escola")
public record SchoolRequestDTO(

    @NotBlank
    @Schema(example = "Escola Central")
    String name,

    @Schema(example = "33012345")
    String inepCode,

    @Schema(
        description = "ID da instituição. Usado principalmente na atualização.",
        example = "1"
    )
    Long institutionId
) {
}