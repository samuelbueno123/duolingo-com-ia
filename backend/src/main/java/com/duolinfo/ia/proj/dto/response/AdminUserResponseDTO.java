package com.duolinfo.ia.proj.dto.response;

public record AdminUserResponseDTO(
    Long id,
    String name,
    String email,
    boolean isAdmin,
    String baseProfileType,
    String effectiveRole,
    String googleId,
    String profilePicture
) {
}
