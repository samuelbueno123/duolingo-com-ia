package com.duolinfo.ia.proj.dto.request;

public record InstitutionAdminRoleRequestDTO(
    boolean institutionAdmin,
    Boolean primaryInstitutionAdmin
) {
}
