package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionRequestDecisionDTO(
    @NotBlank String decision,
    @Size(max = 150) String institutionName
) {
}
