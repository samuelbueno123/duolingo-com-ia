package com.duolinfo.ia.proj.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionRequestCreateDTO(
    @NotBlank @Size(max = 150) String requestedName,
    @NotBlank @Size(max = 150) String requesterName,
    @NotBlank @Email @Size(max = 254) String requesterEmail,
    @Size(max = 100) String city,
    @Size(max = 2) String state,
    @Size(max = 2000) String message
) {
}
