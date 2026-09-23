package com.duolinfo.ia.proj.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma escola")
public record SchoolResponseDTO(

    Long id,

    String name,

    String inepCode,

    Long institutionId
) {
}