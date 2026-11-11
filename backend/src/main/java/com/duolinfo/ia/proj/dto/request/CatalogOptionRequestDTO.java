package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogOptionRequestDTO(
    @NotBlank(message = "O tipo do catálogo é obrigatório (ex: LANGUAGE ou PROFICIENCY_LEVEL)")
    @Size(max = 40)
    String catalogType,

    @NotBlank(message = "O valor da opção não pode ser vazio")
    @Size(max = 100)
    String value,

    boolean active
) {
}
