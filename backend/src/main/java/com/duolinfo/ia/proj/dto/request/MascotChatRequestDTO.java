package com.duolinfo.ia.proj.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MascotChatRequestDTO(
    @NotBlank @Size(max = 1000) String message,
    String activeSection,
    String userRole,
    String currentLanguage,
    List<MascotChatTurnDTO> history
) {
}
