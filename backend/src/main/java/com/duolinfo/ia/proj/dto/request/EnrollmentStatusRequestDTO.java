package com.duolinfo.ia.proj.dto.request;

import com.duolinfo.ia.proj.entity.EnrollmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo estado da matrícula")
public record EnrollmentStatusRequestDTO(
    @NotNull EnrollmentStatus status,
    String notes
) {
}
