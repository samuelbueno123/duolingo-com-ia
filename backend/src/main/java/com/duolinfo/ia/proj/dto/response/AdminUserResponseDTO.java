package com.duolinfo.ia.proj.dto.response;

public record AdminUserResponseDTO(
    Long id,
    String name,
    String email,
    boolean isAdmin,
    String baseProfileType,
    String effectiveRole,
    String googleId,
    String profilePicture,
    String gender,
    boolean superAdmin,
    boolean institutionAdmin,
    boolean primaryInstitutionAdmin,
    Long institutionId,
    String institutionName,
    boolean active
) {
    public AdminUserResponseDTO(
        Long id,
        String name,
        String email,
        boolean isAdmin,
        String baseProfileType,
        String effectiveRole,
        String googleId,
        String profilePicture,
        String gender,
        boolean superAdmin,
        boolean institutionAdmin,
        boolean primaryInstitutionAdmin,
        Long institutionId,
        String institutionName
    ) {
        this(id, name, email, isAdmin, baseProfileType, effectiveRole, googleId, profilePicture, gender, superAdmin, institutionAdmin, primaryInstitutionAdmin, institutionId, institutionName, true);
    }
}
