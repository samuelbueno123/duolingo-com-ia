package com.duolinfo.ia.proj.dto.request;

public record DeleteAccountRequestDTO(
    String currentPassword,
    String confirmationText
) {
}
