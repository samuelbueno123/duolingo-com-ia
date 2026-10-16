package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.NotNull;

public record TeacherApprovalRequestDTO(
    @NotNull Boolean approved
) {
}
