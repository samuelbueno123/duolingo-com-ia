package com.duolinfo.ia.proj.dto.response;

public record MascotChatResponseDTO(
    String reply,
    boolean inScope,
    String suggestedAction
) {
}
