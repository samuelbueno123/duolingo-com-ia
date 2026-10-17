package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TeacherRequestDecisionDTO(
    @NotBlank(message = "A decisão é obrigatória (APPROVE ou REJECT).")
    String decision
) {
}
